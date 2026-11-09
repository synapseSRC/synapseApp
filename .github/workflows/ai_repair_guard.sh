#!/usr/bin/env bash
# =============================================================================
# Synapse Social - shared guard rails for the AI build-repair pipeline.
#
# This file is sourced by ai_build_repair.sh (the in-job repair loop) and by
# ai_commit_repairs.sh (the single consolidated fix commit). It holds the parts
# that MUST behave identically in both places:
#
#   * the redaction filter used on every line we print or store;
#   * the path policy that decides which files a repair is allowed to change;
#   * the change detection used to undo out-of-scope edits and stray commits;
#   * the classifiers that tell a real code error apart from a transient
#     infrastructure/network failure, and that recognise model-gateway
#     availability and authentication errors.
#
# The path policy is enforced in this pipeline, not merely requested from the
# model: anything an agent changes outside the allow-list is reverted before the
# build is retried, so an out-of-scope edit can never reach the final commit.
# =============================================================================

# --- Path policy -------------------------------------------------------------
#
# Allowed: production source of the Android app module and of the shared KMP
# module. Everything else - workflows, Gradle/build configuration, the wrapper,
# signing material, generated google-services.json, test source sets, and any
# build output - is refused.
path_is_allowed() {
  local path="$1"
  [ -n "$path" ] || return 1

  # Never accept absolute paths, path traversal or git's quoted form.
  case "$path" in
    /*|*..*|*'"'*) return 1 ;;
  esac
  path="${path#./}"

  # Only app/ and shared/ source may be touched at all.
  case "$path" in
    app/*|shared/*) ;;
    *) return 1 ;;
  esac

  # Module build configuration, signing material and generated config.
  case "$path" in
    app/build.gradle|app/proguard-rules.pro|app/release_key.jks|app/google-services.json) return 1 ;;
    shared/build.gradle.kts|shared/proguard-rules.pro|shared/google-services.json) return 1 ;;
  esac

  # Test source sets: the pipeline must never ship a build that was fixed by
  # weakening or deleting tests.
  case "$path" in
    */src/*[Tt]est*/*|*/test/*|*/androidTest/*|*/commonTest/*|*/iosTest/*) return 1 ;;
  esac

  # Build scripts, properties, credentials and build outputs.
  case "$path" in
    *.gradle|*.gradle.kts|*.properties|*.jks|*.keystore|*.p12|*.pem|*.key) return 1 ;;
    */.github/*|*/.git/*|*/build/*|*/.gradle/*|*/.kotlin/*) return 1 ;;
  esac

  return 0
}

# Lists every path that differs from HEAD, one per line, including untracked
# files (so a brand-new file the agent created is visible too).
changed_paths() {
  local line path
  git -C "$1" status --porcelain=v1 -uall 2>/dev/null | while IFS= read -r line; do
    [ -n "$line" ] || continue
    path="${line:3}"
    case "$path" in
      *' -> '*) path="${path##* -> }" ;;
    esac
    # Generated build output, caches and IDE state are not repairs: a Gradle run
    # creates them, path_is_allowed() refuses them anyway, and treating them as
    # out-of-scope edits would block the commit of a genuinely green repair on a
    # repository whose .gitignore does not (or no longer) cover them.
    case "$path" in
      build/*|*/build/*|.gradle/*|*/.gradle/*|.kotlin/*|*/.kotlin/*|.idea/*|*/.idea/*) continue ;;
    esac
    printf '%s\n' "$path"
  done
}

# --- Redaction ---------------------------------------------------------------
#
# Every byte we echo, tee or archive goes through this filter. It removes the
# Kilo API key exactly, plus the credential shapes that could realistically
# travel through a build log (JWTs, GitHub tokens, bearer headers).
redact_stream() {
  local key="${KILO_API_KEY:-}"
  local -a sed_args=()
  if [ -n "$key" ]; then
    local escaped
    escaped="$(printf '%s' "$key" | sed -e 's/[\\&|]/\\&/g')"
    sed_args+=(-e "s|${escaped}|***REDACTED***|g")
  fi
  sed "${sed_args[@]}" \
    -e 's#eyJ[A-Za-z0-9_-]\{8,\}\.[A-Za-z0-9_-]\{8,\}\.[A-Za-z0-9_-]\{4,\}#***REDACTED-JWT***#g' \
    -e 's#gh[pousr]_[A-Za-z0-9]\{16,\}#***REDACTED-GH-TOKEN***#g' \
    -e 's#\([Aa]uthorization: *[Bb]earer *\)[^[:space:]]\+#\1***REDACTED***#g' \
    -e 's#\([Ss]et-[Cc]ookie: *\)[^[:space:]]\+#\1***REDACTED***#g'
}

redact_line() { printf '%s\n' "$1" | redact_stream; }

# --- Failure classification --------------------------------------------------
#
# A failing Gradle run is classified as "code" (invoke the repair agent) or
# "infra" (retry the build, keep the AI repair budget for real code errors).
# Compiler/runtime code markers win over infrastructure markers, because a
# compilation error can be accompanied by dependency noise.
classify_failure() {
  local logfile="$1"
  [ -s "$logfile" ] || { printf 'code'; return 0; }

  if grep -Eqi \
    -e '^e: ' \
    -e '^e: file' \
    -e 'error: ' \
    -e 'Unresolved reference' \
    -e 'Type mismatch' \
    -e 'None of the following (functions|candidates)' \
    -e 'AssertionError' \
    -e 'AAPT2' \
    -e 'failed linking references' \
    -e 'Execution failed for task .*(compile|ksp|kapt|process|merge|link|package)' \
    -e 'KSP.*(error|failed)' \
    -- "$logfile"; then
    printf 'code'
    return 0
  fi

  if grep -Eqi \
    -e 'Could not resolve' \
    -e 'Could not (GET|HEAD|download|install|connect|fetch)' \
    -e 'Connection (timed out|reset|refused)' \
    -e 'Read timed out' \
    -e 'Temporary failure in name resolution' \
    -e 'UnknownHostException' \
    -e 'SocketTimeoutException' \
    -e 'SSL(Handshake)?Exception' \
    -e 'Received status code (429|5[0-9][0-9])' \
    -e '50[234] (Bad Gateway|Service Unavailable|Gateway Timeout)' \
    -e 'Plugin \[id' \
    -e 'Could not find .* in any of the following sources' \
    -e 'No space left on device' \
    -e 'OutOfMemoryError' \
    -e 'Java heap space' \
    -e 'Gradle build daemon disappeared' \
    -e 'Timeout waiting to lock' \
    -- "$logfile"; then
    printf 'infra'
    return 0
  fi

  # Unknown shape: treat it as a code error so a real defect still gets a repair
  # attempt, but it is bounded by the global build-attempt cap.
  printf 'code'
}

# --- Model gateway errors ---------------------------------------------------
output_has_auth_error() {
  grep -Eqi \
    -e '\b401\b' \
    -e '\b403\b' \
    -e 'unauthori[sz]ed' \
    -e 'invalid api key' \
    -e 'missing api key' \
    -e 'api key.*(invalid|expired|required)' \
    -e 'authentication (failed|error|required)' \
    -e 'not signed in' \
    -e 'no credentials' \
    -- "$1"
}

output_has_model_error() {
  grep -Eqi \
    -e 'rate ?limit' \
    -e 'too many requests' \
    -e '\b429\b' \
    -e 'quota' \
    -e 'model .*(not (found|available|supported))' \
    -e '(unknown|invalid) model' \
    -e 'no such model' \
    -e 'provider .* not (found|available|configured)' \
    -- "$1"
}
