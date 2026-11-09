#!/usr/bin/env bash
# =============================================================================
# Synapse Social - AI-powered self-healing Android release build.
#
# This script replaces the single `./gradlew assembleRelease` invocation inside
# the existing "Build APKs" job. It runs on the SAME runner, in the SAME
# workspace and with the SAME Gradle caches as the normal build, so every repair
# is verified by a real Gradle build of the modified working tree:
#
#   1. run the release build exactly as the job did before;
#   2. on success: stop. The repair agent is never invoked (acceptance: a green
#      build must not call Kilo);
#   3. on a code failure: capture the Gradle diagnostics, hand them to the Kilo
#      Code CLI in autonomous mode together with the source files named in the
#      error, and let it edit the working tree - in place, no commit, no push,
#      no stash, no reset;
#   4. rebuild the modified working tree, up to AI_REPAIR_MAX_BUILD_ATTEMPTS
#      (default 5) build attempts in total;
#   5. on success: verify the APK artifacts exist and hand back to the workflow,
#      which runs the existing BuildConfig verification and then creates the
#      single consolidated fix commit;
#   6. on exhaustion: exit non-zero so the job (and the workflow) reports
#      failure, and NOTHING is committed.
#
# Transient infrastructure failures (dependency repositories down, DNS, TLS,
# timeouts, runner resource exhaustion) are classified separately: the build is
# retried without spending an AI repair attempt on them.
#
# Every out-of-scope edit the agent makes is reverted by the path policy in
# ai_repair_guard.sh, and every line this script prints or archives is filtered
# through the redaction filter. The agent runs with a scrubbed environment
# (env -i), so it never sees GITHUB_TOKEN or any repository secret.
#
# Environment:
#   KILO_API_KEY                  Kilo Gateway API key (secret). Empty disables
#                                 AI repair: the loop then reports the build
#                                 failure instead of silently skipping a repair.
#   AI_REPAIR_MODEL               optional model override, provider/model form.
#   AI_REPAIR_FALLBACK_MODELS     optional space-separated extra fallbacks.
#   AI_REPAIR_MAX_BUILD_ATTEMPTS  total build attempts (default 5).
#   AI_REPAIR_MAX_INFRA_RETRIES   infra-only retries inside that cap (default 2).
#   AI_REPAIR_REPAIR_TIMEOUT_SECONDS  wall clock bound per agent run (1500).
#   AI_REPAIR_GRADLE_TASK / AI_REPAIR_GRADLE_FLAGS  build command override.
# =============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
# shellcheck source=ai_repair_guard.sh
. "${SCRIPT_DIR}/ai_repair_guard.sh"

WORKSPACE="${GITHUB_WORKSPACE:-$PWD}"
DIAG_DIR="${AI_REPAIR_DIAG_DIR:-${RUNNER_TEMP:-${TMPDIR:-/tmp}}/kilo-repair}"
STEP_SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"
STEP_OUTPUT="${GITHUB_OUTPUT:-/dev/null}"

MAX_BUILD_ATTEMPTS="${AI_REPAIR_MAX_BUILD_ATTEMPTS:-5}"
MAX_INFRA_RETRIES="${AI_REPAIR_MAX_INFRA_RETRIES:-2}"
INFRA_BACKOFF_SECONDS="${AI_REPAIR_INFRA_BACKOFF_SECONDS:-30}"
REPAIR_TIMEOUT_SECONDS="${AI_REPAIR_REPAIR_TIMEOUT_SECONDS:-1500}"
PROMPT_TAIL_LINES="${AI_REPAIR_PROMPT_TAIL_LINES:-350}"
GRADLE_TASK="${AI_REPAIR_GRADLE_TASK:-assembleRelease}"
GRADLE_FLAGS="${AI_REPAIR_GRADLE_FLAGS:---no-daemon --console=plain -x :app:checkReleaseAarMetadata}"
GRADLE_LOG_TAIL_LINES="${AI_REPAIR_GRADLE_LOG_TAIL_LINES:-200}"
KILO_CONFIG_NAME="${AI_REPAIR_KILO_CONFIG:-kilo_repair_config.jsonc}"
# GitHub Actions surfaces at most 10 error annotations per step, so the diagnostics of
# every attempt are streamed as plain log text and stored per attempt, while the step's
# annotation budget is reserved for the loop's own verdict plus the last attempt.
MAX_DIAGNOSTIC_ANNOTATIONS="${AI_REPAIR_MAX_DIAGNOSTIC_ANNOTATIONS:-6}"

KILO_BIN=""
MODELS=()
LAST_MODEL=""
ABORTED_FOR_AUTH=0

log()  { printf '[ai-repair] %s\n' "$*"; }
warn() { printf '[ai-repair] warning: %s\n' "$*" >&2; }
emit_output() { printf '%s=%s\n' "$1" "$2" >>"$STEP_OUTPUT"; }
summary() { printf '%s\n' "$*" >>"$STEP_SUMMARY"; }

# --- Kilo Code CLI discovery -------------------------------------------------
resolve_kilo_bin() {
  local candidate
  if [ -n "${AI_REPAIR_KILO_BIN:-}" ] && command -v "$AI_REPAIR_KILO_BIN" >/dev/null 2>&1; then
    printf '%s' "$AI_REPAIR_KILO_BIN"; return 0
  fi
  for candidate in kilo kilocode; do
    if command -v "$candidate" >/dev/null 2>&1; then printf '%s' "$candidate"; return 0; fi
  done
  for candidate in "$HOME/.kilo/bin/kilo" "$HOME/.local/bin/kilo" "/usr/local/bin/kilo"; do
    if [ -x "$candidate" ]; then printf '%s' "$candidate"; return 0; fi
  done
  return 1
}

install_kilo_cli() {
  # Lazy: a green build never installs anything. Only reached when the build has
  # already failed with a code error, so a broken/absent agent costs nothing on
  # healthy runs. Both install paths are the ones Kilo documents.
  [ -n "$KILO_BIN" ] && return 0
  local version="${AI_REPAIR_KILO_VERSION:-}"
  local linklog="$DIAG_DIR/kilo-install.log"

  log "installing the Kilo Code CLI (official installer)"
  if ! timeout 600 bash -c 'curl -fsSL https://kilo.ai/cli/install | bash' >>"$linklog" 2>&1; then
    warn "official installer failed; see $linklog"
  fi
  export PATH="$HOME/.kilo/bin:$PATH"
  KILO_BIN="$(resolve_kilo_bin || true)"

  if [ -z "$KILO_BIN" ] && command -v npm >/dev/null 2>&1; then
    log "installing the Kilo Code CLI with npm (@kilocode/cli${version:+@$version})"
    if ! timeout 600 npm install -g --no-audit --no-fund "@kilocode/cli${version:+@$version}" >>"$linklog" 2>&1; then
      warn "npm install failed; see $linklog"
    fi
    KILO_BIN="$(resolve_kilo_bin || true)"
  fi

  if [ -n "$KILO_BIN" ]; then
    log "Kilo Code CLI available at $KILO_BIN ($(timeout 60 "$KILO_BIN" --version 2>/dev/null | head -n 1 || printf 'version unknown'))"
    install_kilo_config
    catalog_kilo_models
    return 0
  fi

  warn "the Kilo Code CLI could not be installed; AI repair is unavailable"
  return 1
}

install_kilo_config() {
  local src="$SCRIPT_DIR/$KILO_CONFIG_NAME" dst="$HOME/.config/kilo/kilo.jsonc"
  if [ ! -f "$src" ]; then
    warn "Kilo config '$src' not found; the CLI will run with its own defaults"
    return 0
  fi
  mkdir -p -- "$(dirname -- "$dst")"
  cp -- "$src" "$dst"
  log "Installed Kilo config from $KILO_CONFIG_NAME (model + permission policy)"
}

# --- Model selection ---------------------------------------------------------
# Candidates are tried in order; a rate limited or unavailable model advances
# the chain instead of costing a build attempt. Free-tier models are rate limited
# upstream (Kilo documents ~200 requests/hour for kilo-auto/free), so the chain,
# not unlimited availability, is what keeps the loop working.
catalog_kilo_models() {
  [ -n "$KILO_BIN" ] || return 0
  timeout 120 "$KILO_BIN" models --verbose >"$DIAG_DIR/kilo-models.txt" 2>&1 || warn "could not list models with 'kilo models'"
}

# Free models are rotated upstream and the free tier is rate limited, so the
# candidate chain is not "one free model and hope": ids that the CLI's own model
# catalog advertises as free are appended verbatim (never invented) after the
# documented default, and a rate limited model advances the chain instead of
# costing a build attempt.
discover_free_models() {
  local catalog="$DIAG_DIR/kilo-models.txt"
  [ -s "$catalog" ] || return 0
  grep -oE '[A-Za-z0-9][A-Za-z0-9._-]*/[A-Za-z0-9][A-Za-z0-9._:/-]*' "$catalog" 2>/dev/null \
    | grep -i -- 'free' | sort -u | head -n 4
}

kilo_advertises_model() {
  local model="$1" catalog="$DIAG_DIR/kilo-models.txt"
  [ -s "$catalog" ] || return 1
  grep -qF -- "$model" "$catalog" || grep -qF -- "${model#*/}" "$catalog"
}

resolve_models() {
  local m
  [ -n "${AI_REPAIR_MODEL:-}" ] && MODELS+=("$AI_REPAIR_MODEL")
  MODELS+=("kilo/kilo-auto/free")
  if [ -n "${AI_REPAIR_FALLBACK_MODELS:-}" ]; then
    read -r -a _extra <<<"$AI_REPAIR_FALLBACK_MODELS"
    MODELS+=("${_extra[@]}")
  fi
  while IFS= read -r m; do
    [ -n "$m" ] && MODELS+=("$m")
  done < <(discover_free_models)
  local -A seen=() ; local -a unique=()
  for m in "${MODELS[@]}"; do
    [ -n "$m" ] || continue
    [ -n "${seen[$m]:-}" ] && continue
    seen[$m]=1
    unique+=("$m")
  done
  MODELS=("${unique[@]:0:6}")
  for m in "${MODELS[@]}"; do
    if kilo_advertises_model "$m"; then
      log "model candidate: $m (advertised by 'kilo models')"
    else
      log "model candidate: $m"
    fi
  done
}

# --- Gradle ------------------------------------------------------------------
extract_gradle_diagnostics() {
  local logfile="$1" outfile="$2" diagnostics="" line
  : >"$outfile"
  [ -s "$logfile" ] || return 0
  diagnostics="$(grep -E '(^e: |^FAILURE:|^\* What went wrong:|^Execution failed for task|^Task .*not found|UnknownTaskException|^> Task .* FAILED|^.* FAILED$|^.*AssertionError:|^.*error:|^.*AAPT2|^.*failed linking references)' -- "$logfile" | tail -n 25 || true)"
  if [ -z "$diagnostics" ]; then
    diagnostics="$(awk 'BEGIN { remaining = 0 } /^\* What went wrong:/ { remaining = 12 } remaining > 0 { print; remaining-- }' "$logfile")"
  fi
  if [ -z "$diagnostics" ]; then
    diagnostics="$(tail -n 12 -- "$logfile")"
  fi
  while IFS= read -r line; do
    [ -n "$line" ] && printf '%s\n' "$line"
  done <<<"$diagnostics" | redact_stream >"$outfile"
}

print_gradle_diagnostics() {
  local outfile="$1"
  [ -s "$outfile" ] || return 0
  printf '[ai-repair] extracted diagnostics:\n'
  sed -e 's/^/    /' -- "$outfile"
}

# Annotations are a budget, not a stream: GitHub shows at most 10 error annotations per
# step, so annotating every attempt would hide the loop's verdict - the one thing an
# operator has to read - behind the first attempt's compiler output.
emit_diagnostic_annotations() {
  local outfile="$1" n=0 line
  [ -s "$outfile" ] || return 0
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    printf '::error title=Android release-build diagnostics::%s\n' "$line"
    n=$((n + 1))
    [ "$n" -ge "$MAX_DIAGNOSTIC_ANNOTATIONS" ] && break
  done <"$outfile"
}

run_gradle() {
  local logfile="$1" stream="$2" status=0
  log "Running: ./gradlew ${GRADLE_TASK} ${GRADLE_FLAGS}"
  if [ "$stream" = "1" ]; then
    ( cd -- "$WORKSPACE" && env -u KILO_API_KEY ./gradlew "$GRADLE_TASK" $GRADLE_FLAGS ) 2>&1 \
      | redact_stream | tee -- "$logfile"
    status="${PIPESTATUS[0]}"
  else
    ( cd -- "$WORKSPACE" && env -u KILO_API_KEY ./gradlew "$GRADLE_TASK" $GRADLE_FLAGS ) >"$logfile" 2>&1
    status=$?
    tail -n "$GRADLE_LOG_TAIL_LINES" -- "$logfile" | redact_stream
  fi
  if [ "$status" -ne 0 ]; then
    local diagfile="${logfile%.log}-diagnostics.txt"
    extract_gradle_diagnostics "$logfile" "$diagfile"
    print_gradle_diagnostics "$diagfile"
  fi
  return "$status"
}

verify_apk_artifacts() {
  local dir="$WORKSPACE/app/build/outputs/apk/release" count=0
  count="$(find "$dir" -maxdepth 1 -name 'app-*-release.apk' 2>/dev/null | wc -l | tr -d ' ')"
  log "release APK artifacts found: $count"
  APK_COUNT="$count"
  if [ "${count:-0}" -lt 1 ]; then
    printf '::error title=APK artifacts::the build succeeded but produced no app-*-release.apk\n'
    return 1
  fi
  if [ "${count:-0}" -lt 3 ]; then
    warn "expected 3 ABI splits (arm64-v8a, armeabi-v7a, x86_64) but found $count APK(s)"
  fi
  return 0
}

# --- Prompt ------------------------------------------------------------------
error_files() {
  local logfile="$1" path
  grep -oE 'file:///[^:[:space:]]+\.(kt|kts|java|xml)' -- "$logfile" 2>/dev/null \
    | sed -e 's#^file://##' | sort -u | head -n 40 \
    | while IFS= read -r path; do
        case "$path" in
          "$WORKSPACE"/*) printf '%s\n' "${path#"$WORKSPACE"/}" ;;
          *) printf '%s\n' "$path" ;;
        esac
      done
}

build_prompt() {
  local attempt="$1" logfile="$2" kind="$3" out="$4" path listed=0
  local model="${LAST_MODEL:-${MODELS[0]:-unknown}}"

  {
    printf 'You are repairing a failing Android / Kotlin Multiplatform RELEASE build in the\n'
    printf 'Synapse Social repository. The working directory is the repository root and CI ran:\n\n'
    printf '    ./gradlew %s %s\n\n' "$GRADLE_TASK" "$GRADLE_FLAGS"
    printf 'That command failed. Diagnose the ROOT CAUSE from the diagnostics below and fix the\n'
    printf 'source code in this working tree so that the same command succeeds.\n\n'

    printf 'Context\n-------\n'
    printf 'build attempt: %s of %s\n' "$attempt" "$MAX_BUILD_ATTEMPTS"
    printf 'failure classification: %s\n' "$kind"
    printf 'agent model: %s\n\n' "$model"

    printf 'Repository rules (mandatory)\n---------------------------\n'
    printf '* Follow AGENTS.md and REVIEW.md: Kotlin Multiplatform, Clean Architecture + MVVM,\n'
    printf '  Koin DI, Jetpack Compose, UseCases own business logic, DTOs need mappers,\n'
    printf '  no android.*/java.* imports in shared/commonMain.\n'
    printf '* Make the smallest correct change. Do not refactor unrelated code, do not reformat\n'
    printf '  files, do not rename public APIs, do not add or upgrade dependencies.\n'
    printf '* Never add, modify, delete, skip or weaken tests, and never run test tasks\n'
    printf '  (./gradlew test, testDebugUnitTest, connectedAndroidTest, ...).\n'
    printf '* Never touch .github/**, gradle/**, gradlew, gradle.properties, keystore.properties,\n'
    printf '  *.jks, *.keystore, local.properties, app/google-services.json, or any *.gradle /\n'
    printf '  *.gradle.kts build script. Edits outside app/ and shared/ are reverted automatically.\n'
    printf '* Do NOT run git commit, git push, git stash, git reset, git checkout or branching:\n'
    printf '  leave the fix in the working tree only. The pipeline commits once, after the build\n'
    printf '  and the release verification pass.\n'
    printf '* Do not print, copy or transmit credentials; none are available in this environment.\n'
    printf '* You may run the allow-listed compile task to check yourself:\n'
    printf '  ./gradlew :app:compileReleaseKotlin   (or :shared:compileReleaseKotlin)\n'
    printf '  Every other shell command is denied.\n\n'

    printf 'Files named in the compiler output\n----------------------------------\n'
    while IFS= read -r path; do
      [ -n "$path" ] || continue
      case "$path" in
        */build/*|*/.gradle/*) continue ;;
      esac
      if [ -f "$WORKSPACE/$path" ]; then
        printf '%s (read this file)\n' "$path"
        listed=$((listed + 1))
      else
        printf '%s (not present in the working tree)\n' "$path"
      fi
      [ "$listed" -ge 25 ] && break
    done < <(error_files "$logfile")
    [ "$listed" -eq 0 ] && printf '(the log did not name a source file; inspect the modules the failing task belongs to)\n'
    printf '\n'

    printf 'Compiler and task error lines\n-----------------------------\n'
    grep -E '^e: |^e: file|error:|Unresolved reference|Type mismatch|^Execution failed for task|^\* What went wrong:|KSP|AAPT2' -- "$logfile" \
      | tail -n 60 | redact_stream
    printf '\n'

    printf '"What went wrong" block\n-----------------------\n'
    awk 'BEGIN { remaining = 0 } /^\* What went wrong:/ { remaining = 45 } remaining > 0 { print; remaining-- }' "$logfile" | redact_stream
    printf '\n'

    printf 'Tail of the Gradle log (last %s lines)\n' "$PROMPT_TAIL_LINES"
    printf -- '--------------------------------------\n'
    tail -n "$PROMPT_TAIL_LINES" -- "$logfile" | redact_stream
    printf '\n'

    printf 'Deliverable\n-----------\n'
    printf 'Edit the working tree so that ./gradlew %s compiles and passes. Explain the root\n' "$GRADLE_TASK"
    printf 'cause briefly, then stop. Do not commit.\n'
  } >"$out"
}

# --- Guards ------------------------------------------------------------------
guard_workspace() {
  local baseline="$1" path head_now
  head_now="$(git -C "$WORKSPACE" rev-parse HEAD)"
  if [ "$head_now" != "$baseline" ]; then
    warn "the agent created a commit ($head_now); undoing it and keeping its working-tree changes"
    printf '::warning title=AI repair::the repair agent committed during the loop; the commit was undone so the pipeline can create the single consolidated commit\n'
    git -C "$WORKSPACE" reset --mixed --quiet "$baseline" || warn "could not undo the agent commit"
  fi

  while IFS= read -r path; do
    [ -n "$path" ] || continue
    path_is_allowed "$path" && continue
    warn "reverting out-of-scope change: $path"
    if git -C "$WORKSPACE" ls-files --error-unmatch -- "$path" >/dev/null 2>&1; then
      git -C "$WORKSPACE" checkout -- "$path" 2>/dev/null || warn "could not revert $path"
    else
      rm -f -- "$WORKSPACE/$path" 2>/dev/null || warn "could not remove $path"
    fi
    printf '::warning title=AI repair::reverted an out-of-scope change by the repair agent: %s\n' "$path"
  done < <(changed_paths "$WORKSPACE")
}

# --- Agent invocation --------------------------------------------------------
invoke_repair() {
  local attempt="$1" promptfile="$2" outfile="$3" idx=0 rc=0 model prompt_text
  local -a env_args=()

  prompt_text="$(cat -- "$promptfile")"
  while :; do
    model="${MODELS[$idx]:-}"
    if [ -z "$model" ]; then
      warn "no model candidates left"
      return 1
    fi
    LAST_MODEL="$model"

    env_args=(
      "PATH=$PATH" "HOME=$HOME" "TMPDIR=${TMPDIR:-/tmp}" "LANG=${LANG:-C.UTF-8}"
      "CI=true" "TERM=dumb" "KILO_API_KEY=${KILO_API_KEY:-}" "KILO_ORG_ID=${KILO_ORG_ID:-}"
    )
    for var in HTTP_PROXY http_proxy HTTPS_PROXY https_proxy NO_PROXY no_proxy \
               SSL_CERT_FILE SSL_CERT_DIR XDG_CONFIG_HOME XDG_DATA_HOME XDG_CACHE_HOME; do
      [ -n "${!var:-}" ] && env_args+=("${var}=${!var}")
    done

    log "invoking Kilo Code CLI ($KILO_BIN run --auto) attempt $attempt with model $model (timeout ${REPAIR_TIMEOUT_SECONDS}s)"
    : >"$outfile"
    ( cd -- "$WORKSPACE" && env -i "${env_args[@]}" \
        timeout --signal=TERM --kill-after=60 "${REPAIR_TIMEOUT_SECONDS}" \
        "$KILO_BIN" run --auto --model "$model" \
          --title "ci-build-repair-attempt-${attempt}" \
          "$prompt_text" ) 2>&1 | redact_stream | tee -- "$outfile"
    rc="${PIPESTATUS[0]}"
    log "Kilo exit status: $rc"

    if [ "$rc" -eq 0 ]; then
      return 0
    fi
    if [ "$rc" -eq 124 ] || [ "$rc" -eq 137 ]; then
      warn "the agent run was killed after ${REPAIR_TIMEOUT_SECONDS}s"
      summary "- attempt ${attempt}: agent run timed out after ${REPAIR_TIMEOUT_SECONDS}s"
      return 1
    fi
    if output_has_auth_error "$outfile"; then
      printf '::error title=Kilo Code CLI::the model gateway rejected the request (HTTP 401/403 or missing/invalid API key); check the KILO_API_KEY secret\n'
      ABORTED_FOR_AUTH=1
      return 2
    fi
    if output_has_model_error "$outfile" && [ "$((idx + 1))" -lt "${#MODELS[@]}" ]; then
      idx=$((idx + 1))
      warn "model '$model' is unavailable or rate limited; falling back to '${MODELS[$idx]}'"
      summary "- attempt ${attempt}: model \`$model\` unavailable/rate-limited, retrying with \`${MODELS[$idx]}\`"
      continue
    fi
    return 1
  done
}

# --- Main --------------------------------------------------------------------
main() {
  local attempt=0 status=0 kind="" logfile="" promptfile="" kilofile=""
  local ai_repairs=0 infra_retries=0 repairs_applied=false baseline_sha=""
  local last_failure_kind="" repaired=0
  APK_COUNT=0
  FAILURE_REASON=""

  mkdir -p -- "$DIAG_DIR"
  cd -- "$WORKSPACE" || { printf '::error title=AI repair::workspace %s not found\n' "$WORKSPACE"; exit 1; }
  baseline_sha="$(git -C "$WORKSPACE" rev-parse HEAD)"
  log "workspace: $WORKSPACE (HEAD ${baseline_sha:0:12})"

  KILO_BIN="$(resolve_kilo_bin || true)"
  if [ -n "$KILO_BIN" ]; then
    log "Kilo Code CLI: $KILO_BIN ($(timeout 60 "$KILO_BIN" --version 2>/dev/null | head -n 1 || printf 'version unknown'))"
    install_kilo_config
    catalog_kilo_models
  else
    log "no Kilo Code CLI on PATH; it is installed only if the build actually fails"
  fi

  summary "### AI build repair"
  summary ""
  summary "| attempt | result | classification | agent |"
  summary "| --- | --- | --- | --- |"

  while [ "$attempt" -lt "$MAX_BUILD_ATTEMPTS" ]; do
    attempt=$((attempt + 1))
    logfile="$DIAG_DIR/attempt-${attempt}-gradle.log"
    log "=== build attempt ${attempt}/${MAX_BUILD_ATTEMPTS} ==="

    if [ "$attempt" -eq 1 ]; then
      run_gradle "$logfile" 1
    else
      run_gradle "$logfile" 0
    fi
    status=$?

    if [ "$status" -eq 0 ]; then
      log "build succeeded on attempt ${attempt}"
      summary "| ${attempt} | build passed | - | - |"
      break
    fi

    kind="$(classify_failure "$logfile")"
    last_failure_kind="$kind"
    log "attempt ${attempt} failed (classification: ${kind})"

    if [ "$kind" = "infra" ]; then
      summary "| ${attempt} | failed | infrastructure | not invoked |"
      if [ "$infra_retries" -ge "$MAX_INFRA_RETRIES" ]; then
        warn "infrastructure retry budget ($MAX_INFRA_RETRIES) exhausted"
        FAILURE_REASON="the build kept failing on infrastructure problems (dependency repositories, network or runner resources) and the retry budget (${MAX_INFRA_RETRIES}) is exhausted"
        break
      fi
      infra_retries=$((infra_retries + 1))
      if [ "$attempt" -ge "$MAX_BUILD_ATTEMPTS" ]; then
        break
      fi
      log "transient infrastructure failure; retrying the build in ${INFRA_BACKOFF_SECONDS}s without spending an AI repair attempt"
      sleep "$INFRA_BACKOFF_SECONDS"
      continue
    fi

    summary "| ${attempt} | failed | source code | pending |"

    if [ -z "${KILO_API_KEY:-}" ]; then
      warn "KILO_API_KEY is not configured; no repair can be attempted"
      FAILURE_REASON="the build failed and KILO_API_KEY is not configured, so no repair could be attempted; add the repository secret to enable the self-healing loop"
      break
    fi
    if [ -z "$KILO_BIN" ] && ! install_kilo_cli; then
      warn "the Kilo Code CLI is unavailable on this runner"
      FAILURE_REASON="the build failed and the Kilo Code CLI is unavailable on this runner, so no repair could be attempted"
      break
    fi
    if [ "$ai_repairs" -ge "$((MAX_BUILD_ATTEMPTS - 1))" ]; then
      warn "AI repair budget exhausted"
      FAILURE_REASON="the AI repair budget is exhausted (build attempts - 1 repair runs)"
      break
    fi

    [ "${#MODELS[@]}" -gt 0 ] || resolve_models
    promptfile="$DIAG_DIR/attempt-${attempt}-prompt.txt"
    kilofile="$DIAG_DIR/attempt-${attempt}-kilo.log"
    build_prompt "$attempt" "$logfile" "$kind" "$promptfile"
    log "captured Gradle log $(wc -c <"$logfile" | tr -d ' ') bytes, prompt $(wc -c <"$promptfile" | tr -d ' ') bytes"

    invoke_repair "$attempt" "$promptfile" "$kilofile"
    status=$?
    ai_repairs=$((ai_repairs + 1))

    guard_workspace "$baseline_sha"

    if [ -n "$(changed_paths "$WORKSPACE" | head -n 1)" ]; then
      repairs_applied=true
      repaired=$((repaired + 1))
      log "attempt ${attempt}: working tree now holds uncommitted repair changes"
    else
      log "attempt ${attempt}: the agent left the working tree unchanged"
    fi

    if [ "$status" -eq 2 ] || [ "$ABORTED_FOR_AUTH" -eq 1 ]; then
      warn "aborting the repair loop: the model gateway rejected our credentials"
      FAILURE_REASON="the model gateway rejected the request (HTTP 401/403 or a missing/invalid API key); the repair loop was aborted"
      break
    fi
  done

  emit_output "build_attempts" "$attempt"
  emit_output "ai_repairs" "$ai_repairs"
  emit_output "kilo_model" "$LAST_MODEL"
  emit_output "repairs_applied" "$repairs_applied"

  if [ "$status" -ne 0 ]; then
    verify_apk_artifacts >/dev/null 2>&1 || true
    emit_output "apk_count" "$APK_COUNT"
    summary ""
    summary "the release build did **not** pass after ${attempt} attempt(s) (classification of the last failure: ${last_failure_kind}); no commit was created."
    printf '::error title=AI repair::%s\n' "${FAILURE_REASON:-the release build did not pass within the attempt budget}"
    printf '::error title=AI repair::release build still failing after %s attempt(s); no repair commit was created\n' "$attempt"
    emit_diagnostic_annotations "$DIAG_DIR/attempt-${attempt}-gradle-diagnostics.txt"
    exit 1
  fi

  if ! verify_apk_artifacts; then
    emit_output "apk_count" "$APK_COUNT"
    summary ""
    summary "the build command exited 0 but no release APK was produced; refusing to treat this as a successful repair."
    exit 1
  fi
  emit_output "apk_count" "$APK_COUNT"

  log "build passed on attempt ${attempt} (agent repairs: ${ai_repairs}, repaired attempts: ${repaired})"
  summary ""
  if [ "$repairs_applied" = "true" ]; then
    summary "build passed on attempt ${attempt} after ${repaired} AI repair(s); the workflow now runs the existing release verification and then creates the single consolidated fix commit."
  else
    summary "build passed on attempt ${attempt}; the repair agent was not needed."
  fi
  summary ""
  summary "diagnostics for every attempt: artifact \`ai-repair-diagnostics-*\` (logs, prompts and agent output, redacted)."
  exit 0
}

main "$@"
