# FAQ — SynapseApp Autonomous UX Workflow

This file contains operational answers for common edge cases. The entrypoint `main.md` intentionally links here instead of duplicating FAQ answers.

## What if the task-management integration is unavailable?

Do not pretend tasks were created. Use supported GitHub Issues and PR records as the available source of truth, continue only with actions that can be performed safely, and disclose the task-management limitation in the execution report.

## What if the daily integration branch already exists?

Fetch and reuse it. Preserve its history and integrated changes. Never reset, recreate, replace it with `main`, or force-push it.

## What if two executions create the same daily branch at nearly the same time?

Fetch the remote branch after the creation race and reuse the branch that exists. Do not overwrite it or attempt to replace its history.

## What if a candidate bug already has an equivalent GitHub Issue?

Do not create a duplicate. Link the existing issue in the execution task/report. Comment only when there is genuinely useful new technical evidence.

## What if an issue is confirmed but cannot be fixed safely in this run?

Keep the issue open. Record why implementation is deferred or blocked, update the bug task, and do not create a PR for an unimplemented change.

## What if more than two bugs are found?

Rank candidates by user impact and evidence strength. Create no more than two new issues and implement no more than two fixes per execution. Record out-of-limit findings as deferred without exceeding the limits.

## What if a PR is waiting for required checks or approval?

Leave it open and do not merge. Record the actual pending condition and keep the related task in the appropriate review or blocked state. Do not bypass protection or approvals.

## What if GitHub Actions fails?

Inspect the failed job logs and determine whether the PR change caused the failure. Correct only understood and safely fixable causes on the same feature branch. For external or unrelated failures, document the blocker and do not merge while required checks are failing or unknown.

## What if a merge conflict is unclear or risky?

Do not guess. Preserve both sides, leave the PR open, document the conflicting files and the decision needed, and mark the task blocked.

## What if the PR is merged but feature-branch deletion fails?

Report the merge as successful only if GitHub confirms it and the merge commit/integrated changes are verified. Preserve the branch, record the actual deletion failure, and leave the cleanup task blocked. Do not mark the overall bug lifecycle complete.

## What if branch deletion cannot be independently verified?

Treat deletion as unconfirmed. Do not claim cleanup success, and preserve/report the branch state for manual follow-up.

## Can the daily integration branch be merged into `main` automatically?

No. This workflow only integrates bug-fix PRs into the daily date-based branch. It must never automatically merge that daily branch into `main`.

## What if a command, API, or GitHub capability is unavailable?

Do not simulate the result or claim success. Record what could not be performed, use an available safe alternative only when it preserves the same safeguards, and report the limitation.

## When is a bug task truly complete?

Only after the fix has been reviewed, relevant available verification is successful, its dedicated PR is confirmed squash-merged into the intended daily branch, the merge commit and integrated changes are verified, and the exact feature branch is confirmed deleted.
