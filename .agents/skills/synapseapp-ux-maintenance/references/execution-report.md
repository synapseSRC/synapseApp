# Execution Report

Produce a factual report at the end of every execution. Distinguish confirmed results from pending, unavailable, failed, blocked, and deferred work. Do not claim an action succeeded without checking its actual result.

Use this structure:

# SynapseApp Autonomous UX Execution Report

## 1. Repository Inspection

- Repository:
- Inspected branch:
- Starting/current commit SHA:
- Areas explored:
- Relevant implementation paths:

## 2. Task Management

- Execution task URL/ID:
- Subtasks created or reused:
- Tasks updated:
- Completed tasks:
- Blocked/deferred tasks and reasons:
- Task integration availability/limitations:

## 3. Daily Integration Branch

- Execution date (`Asia/Dhaka`):
- Daily branch name:
- Existed or newly created:
- If created, confirmation it originated from latest `main`:
- Remote branch existence verified:

## 4. Bug Findings

For each finding, include:

- Title:
- User impact and priority:
- Evidence (paths, symbols, line references):
- Duplicate search result:
- GitHub Issue URL (or existing duplicate URL):
- Bug task URL/ID:
- Status: fixed / deferred / duplicate / discarded / out of limit:
- Deferral/blocker reason, if applicable:

## 5. Pull Requests

For each PR, include:

- PR title and URL:
- Related issue:
- Source feature branch:
- Daily integration base branch:
- Latest source commit assessed:
- Current PR status:
- Required checks and approval status:
- Conflict status and resolution:
- Squash merge confirmed: yes/no/pending:
- Merge commit SHA, if confirmed:
- Integration verification:
- Feature branch deletion status:
- Exact deleted branch name, or actual deletion failure:

## 6. Code Verification

- Files modified:
- Implementation review outcome:
- Build/lint/format/static-analysis/type-check commands run and actual results:
- Failed, skipped, unavailable, or unrelated checks:

Do not list commands as successful unless they were actually run and passed.

## 7. Conflict Resolution

- Whether conflicts occurred:
- Affected files:
- Resolution performed:
- Relevant verification:
- Remaining manual intervention:

## 8. Final Counts

Report separate totals for:

- New GitHub issues created:
- Existing duplicates found:
- Bug fixes implemented:
- PRs opened:
- PRs squash-merged and verified:
- Feature branches successfully deleted:
- Pending PRs:
- Deferred findings/fixes:
- Blocked cleanup tasks:
- External blockers:

## 9. Final Outcome

State clearly what is complete, what remains pending, what was deferred, and what requires human intervention. Do not close issues prematurely. Ensure task states match the verified repository and GitHub state.
