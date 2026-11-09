#!/usr/bin/env bash
# =============================================================================
# Synapse Social - single consolidated commit for AI build repairs.
#
# Runs as the last step of the "Build APKs" job, AFTER the release build passed
# inside ai_build_repair.sh and AFTER the existing "Verify shipped auth and push
# config" step passed. It therefore cannot run when the build or the release
# verification failed, and no attempt inside the loop ever committed anything.
#
# Behaviour:
#   * no AI changes      -> exits 0 without touching git;
#   * changes in scope   -> one commit containing every successful repair, pushed
#                           to the branch that is being built;
#   * anything out of scope, or a context where pushing would be unsafe
#                           (fork PR head, tag/detached ref) -> hard failure,
#                           nothing is committed.
#
# The push uses the workflow's GITHUB_TOKEN. GitHub does not start new workflow
# runs for events caused by that token, so this commit cannot re-trigger the
# build job and cannot create an endless repair/push cycle.
# =============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
# shellcheck source=ai_repair_guard.sh
. "${SCRIPT_DIR}/ai_repair_guard.sh"

WORKSPACE="${GITHUB_WORKSPACE:-$PWD}"
STEP_SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"
REF_TYPE="${AI_REPAIR_REF_TYPE:-branch}"
HEAD_REF="${AI_REPAIR_HEAD_REF:-}"
BRANCH="${AI_REPAIR_BRANCH:-}"
ACTOR="${AI_REPAIR_ACTOR:-github-actions[bot]}"
ATTEMPTS="${AI_REPAIR_ATTEMPTS:-0}"
MODEL="${AI_REPAIR_MODEL:-unknown}"
BOT_NAME="github-actions[bot]"
BOT_EMAIL="41898282+github-actions[bot]@users.noreply.github.com"

log()  { printf '[ai-commit] %s\n' "$*"; }
warn() { printf '[ai-commit] warning: %s\n' "$*" >&2; }
summary() { printf '%s\n' "$*" >>"$STEP_SUMMARY"; }

cd -- "$WORKSPACE" || { printf '::error title=AI commit::workspace %s not found\n' "$WORKSPACE"; exit 1; }

if [ "${AI_REPAIR_APPLIED:-false}" != "true" ]; then
  log "no AI repairs were applied; nothing to commit"
  exit 0
fi

# --- Refuse to push in unsafe contexts ---------------------------------------
if [ -n "$HEAD_REF" ]; then
  printf '::error title=AI commit::this run is for pull request head branch "%s"; refusing to push a repair commit from that context\n' "$HEAD_REF"
  exit 1
fi
if [ "$REF_TYPE" != "branch" ]; then
  printf '::error title=AI commit::this run was not triggered by a branch (ref_type=%s); refusing to push a repair commit\n' "$REF_TYPE"
  exit 1
fi
if [ -z "$BRANCH" ]; then
  printf '::error title=AI commit::could not determine the branch to push to\n'
  exit 1
fi

# --- Validate the change set -------------------------------------------------
mapfile -t CHANGED < <(changed_paths "$WORKSPACE")
if [ "${#CHANGED[@]}" -eq 0 ]; then
  log "the working tree is clean; nothing to commit"
  exit 0
fi

OUT_OF_SCOPE=()
for path in "${CHANGED[@]}"; do
  [ -n "$path" ] || continue
  path_is_allowed "$path" || OUT_OF_SCOPE+=("$path")
done
if [ "${#OUT_OF_SCOPE[@]}" -gt 0 ]; then
  printf '::error title=AI commit::out-of-scope change(s) present: %s\n' "${OUT_OF_SCOPE[*]}"
  exit 1
fi

log "committing ${#CHANGED[@]} changed path(s):"
printf '  %s\n' "${CHANGED[@]}"

# --- Commit ------------------------------------------------------------------
git config user.name "$BOT_NAME"
git config user.email "$BOT_EMAIL"

# Stage exactly the paths that were validated above. A bare `git add -A -- app shared`
# pathspec aborts the whole add when one of the directories is absent, which would
# leave the repair unstaged and silently drop it.
for path in "${CHANGED[@]}"; do
  [ -n "$path" ] || continue
  git add -A -- "$path" || warn "could not stage $path"
done
if git diff --cached --quiet; then
  log "nothing staged under app/ or shared/; nothing to commit"
  exit 0
fi

git commit --quiet -m "fix(ci): apply AI-assisted Android build repairs" -m \
"The release build failed in the Build APKs workflow and was repaired in-run by the
Kilo Code CLI in autonomous mode, then rebuilt and verified in the same job.

- build attempts used: ${ATTEMPTS}
- repair model: ${MODEL}
- repaired by: ${BOT_NAME} on behalf of @${ACTOR}

Only source files under app/ and shared/ are included: the repair pipeline reverts
workflow, Gradle, signing, generated-config and test changes, and no attempt inside
the loop commits anything. This is the single consolidated repair commit."

COMMIT_SHA="$(git rev-parse HEAD)"
log "created commit ${COMMIT_SHA:0:12}"

# --- Push --------------------------------------------------------------------
push_once() {
  git push --quiet origin "HEAD:refs/heads/${BRANCH}"
}

if ! push_once; then
  warn "push rejected; rebasing onto origin/${BRANCH} and retrying once"
  if raw="$(git pull --rebase --autostash origin "$BRANCH" 2>&1)"; then
    if ! push_once; then
      printf '::error title=AI commit::could not push the repair commit to %s after a rebase; the build succeeded but the fix was not published\n' "$BRANCH"
      exit 1
    fi
  else
    printf '::error title=AI commit::could not rebase onto origin/%s before pushing\n' "$BRANCH"
    printf '%s\n' "$raw" | redact_stream >&2
    git rebase --abort >/dev/null 2>&1 || true
    exit 1
  fi
fi

log "pushed ${COMMIT_SHA:0:12} to ${BRANCH}"

{
  printf '%s=%s\n' "commit_sha" "$COMMIT_SHA"
  printf '%s=%s\n' "commit_branch" "$BRANCH"
} >>"${GITHUB_OUTPUT:-/dev/null}"

summary ""
summary "consolidated repair commit \`${COMMIT_SHA:0:7}\` pushed to \`${BRANCH}\` (${#CHANGED[@]} file(s), model \`${MODEL}\`)."
exit 0
