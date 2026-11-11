# Daily Integration Branches & Pull Requests

## 1. Daily branch naming

Use the current date in the `Asia/Dhaka` timezone. Format the daily integration branch exactly as `dd-mm-yyyy`, with two-digit day and month values.

Examples: `05-11-2026`, `11-11-2026`, `24-12-2026`.

Only one daily integration branch should be used for a given date.

## 2. Daily branch initialization

Fetch the latest remote references and check whether the exact daily branch exists.

**If it exists:**

- Fetch and use the existing remote branch.
- Preserve its history and integrated changes.
- Never reset, recreate, replace with `main`, or overwrite it.
- Do not force-push it.

**If it does not exist:**

- Fetch the latest remote `main`.
- Create the daily branch from that latest `main`.
- Push it and confirm the remote branch exists.
- If another execution creates it concurrently, fetch and reuse that branch; do not overwrite it.

The daily integration branch is a staging/integration target. This workflow must never automatically merge it into `main`.

## 3. One feature branch per bug

Each independently fixable bug gets its own feature branch, created from the latest daily integration branch. Use a clear name such as:

`fix/button-navigation-11-11-2026`

Use a concise, unique bug slug. If the proposed branch name already exists, inspect its associated work before proceeding. Reuse it only when it is demonstrably the same in-progress fix; otherwise select a distinct safe name. Never overwrite unrelated work.

Commit and push only the changes for that bug. Never combine unrelated fixes in one PR.

## 4. PR targeting and limits

- Every bug-fix PR must target the current daily integration branch.
- Never open an autonomous bug-fix PR directly against `main`.
- Maximum 2 dedicated PRs per execution, one per bug.
- Confirm the source and target branch names after creating the PR.

Example:

| Field | Value |
|---|---|
| Source branch | `fix/dialog-dismissal-11-11-2026` |
| Target branch | `11-11-2026` |

## 5. PR title

Use:

`fix: concise description of the resolved bug`

## 6. Required PR description

### Summary
Explain the defect and its underlying cause.

### Related Issue
Provide the corresponding GitHub Issue link.

### Changes
Describe the actual code modifications and relevant files.

### Verification
Summarize the implementation review and the actual verification performed, including failures or unavailable checks.

### User Impact
Explain the improvement experienced by users.

### Acceptance Criteria
Explain how the correction satisfies the linked issue's acceptance criteria.

## 7. After opening the PR

Record the PR URL, number, source branch, base branch, and linked issue in the bug task and execution task. Update task state to the appropriate review/waiting state. Then follow [pr-lifecycle.md](pr-lifecycle.md) for checks, approvals, conflicts, merge, and cleanup.
