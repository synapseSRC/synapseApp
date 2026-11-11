# Pull Request Lifecycle: CI, Conflicts, Merge & Cleanup

Manage each dedicated bug-fix PR through its full lifecycle. Opening a PR is not completion.

## 1. Monitor GitHub Actions and required checks

For every PR:

1. Retrieve and record the PR URL and current source/base branches.
2. Inspect associated GitHub Actions workflows and required status checks.
3. Monitor active runs until their actual status is known.
4. Inspect required review/approval requirements and blocking review conversations.
5. Re-check the latest source commit before making a merge decision.

Never invent workflow results or report pending checks as passed.

### Failed checks

When a workflow fails:

1. Inspect the failed job and logs.
2. Identify the underlying cause.
3. Determine whether the PR changes introduced it.
4. If the cause is understood and safely correctable, fix it on the same feature branch.
5. Commit and push the correction without rewriting shared history recklessly.
6. Wait for the new checks and assess their actual results.

If a failure is caused by external infrastructure, unavailable secrets, or unrelated pre-existing conditions, document the evidence and blocker. Leave the PR open and do not merge while required checks are failing or unknown.

Never disable workflows, weaken checks, bypass branch protection, dismiss required checks, or change unrelated workflow configuration to obtain a green status.

## 2. Conflict detection and resolution

When a PR conflicts with the daily integration branch:

1. Fetch the latest daily integration branch and feature branch.
2. Identify all conflicting files.
3. Inspect both versions and understand the intent of each change.
4. Use the issue requirements and surrounding implementation to determine intended behavior.

Resolve only when the correct result is sufficiently clear:

- Preserve necessary changes from both sides.
- Protect previously integrated fixes and unrelated work.
- Make the smallest appropriate resolution.
- Never blindly choose one side or discard changes merely to clear the conflict.
- Never modify the daily branch directly to force the PR through.

After resolving on the existing feature branch:

1. Inspect every affected file and the full resulting diff.
2. Perform relevant available verification.
3. Commit and push the resolution to the same feature branch.
4. Wait for GitHub Actions to run again.
5. Reassess checks, approvals, and conflicts from the latest PR state.

### Unsafe conflicts

Do not resolve automatically if intended behavior is unclear, business logic is incompatible, user data or security may be affected, migrations/persisted data may be damaged, a product decision is needed, or the result cannot be adequately verified.

Leave the PR open, preserve both sides, document the conflicting files and required manual decision, and mark the task `BLOCKED`.

## 3. Squash-and-merge gate

Squash and Merge is the only permitted merge method for this workflow. Before merging, confirm all of the following against the latest PR state:

- The PR targets the intended current daily integration branch.
- All required GitHub Actions workflows and status checks have passed.
- Required reviews and approvals are satisfied.
- No blocking review conversations remain unresolved.
- No merge conflicts exist.
- The implementation review and relevant verification have been completed successfully.
- The source commit being merged is the latest commit whose checks and reviews were assessed.
- No new changes have invalidated the previous assessment.

Never bypass branch protection, approvals, or required checks. If any condition is false or unknown, leave the PR open and record the blocker.

## 4. Merge and verify

When every gate is satisfied:

1. Retrieve the latest PR details.
2. Confirm the source commit and intended daily base branch.
3. Reconfirm required checks and approvals.
4. Execute GitHub Squash and Merge with a meaningful squash commit message.
5. Retrieve the updated PR state.
6. Confirm GitHub reports the PR as `MERGED`.
7. Record the resulting merge commit SHA.
8. Fetch the latest daily branch and confirm the squash commit and expected changes are present.
9. Inspect the integrated changes sufficiently to ensure the intended fix is present.

Issuing a merge command is not proof of success. If merge status or integration cannot be confirmed, do not delete the feature branch.

Never automatically merge the daily integration branch into `main`.

## 5. Delete the merged feature branch

Delete only the exact remote source branch associated with the PR after all of these are verified:

- GitHub reports the PR as `MERGED`.
- The squash merge commit SHA is recorded.
- The PR base is the intended daily integration branch.
- The squash commit and expected changes are present in that daily branch.
- The source branch name is taken from the merged PR itself, not guessed from a naming pattern.

Then:

1. Request deletion of that exact remote feature branch using the available GitHub capability.
2. Confirm the branch no longer exists remotely.
3. Record the branch name and verified deletion result.
4. Update the cleanup and bug tasks.

### Deletion safeguards

- Never delete a branch before its PR is confirmed merged.
- Never delete a branch for an open, draft, closed-but-unmerged, blocked, or unverified PR.
- Never delete `main` or the daily integration branch.
- Never delete another execution's in-progress branch.
- Never delete based only on a branch-name pattern.
- Never force-delete or overwrite branches to complete cleanup.
- If deletion fails or cannot be verified, preserve the branch and record the actual failure.

A merged PR with failed cleanup is still a successful merge, but the cleanup task remains `BLOCKED` and the overall bug lifecycle must not be marked fully complete.

## 6. Continue safely

After each verified merge and cleanup, fetch the latest daily branch before creating the next feature branch. Preserve other in-progress PRs and never reset or overwrite the integration branch. Continue only while execution limits permit.
