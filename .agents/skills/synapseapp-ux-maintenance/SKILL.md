---
name: synapseapp-ux-maintenance
description: >-
  Investigates the OpenCrafter/SynapseApp repository for evidence-backed user-facing
  bugs, creates and tracks GitHub issues and execution tasks, implements safe fixes,
  opens dedicated pull requests against a date-based integration branch, monitors CI,
  resolves safe conflicts, squash-merges eligible PRs, and deletes merged feature
  branches. Use when asked to run, schedule, continue, or improve the SynapseApp
  autonomous UX bug-research and self-healing workflow.
compatibility: >-
  Requires repository access, Git operations, GitHub issue/PR/Actions capabilities,
  and a supported task-management mechanism. Capabilities may be unavailable in
  some agent environments; report limitations and never simulate success.
metadata:
  project: SynapseApp
  repository: OpenCrafter/SynapseApp
  version: "1.0.0"
---

# SynapseApp Autonomous UX Maintenance

Run an evidence-driven maintenance cycle for `@github/OpenCrafter/SynapseApp`.
This skill is autonomous within the safeguards below; it does not authorize unsafe
changes, protection bypasses, fabricated verification, or destructive branch operations.

## Fixed configuration

- Repository: `OpenCrafter/SynapseApp`
- Primary branch: `main`
- Daily integration branch: `dd-mm-yyyy`
- Date timezone: `Asia/Dhaka`
- Per execution: at most **2 new GitHub issues**, **2 bug fixes**, and **2 dedicated PRs**
- One independently fixable bug per issue, task, feature branch, and PR
- Every bug-fix PR targets the daily integration branch, never `main`
- Merge method: GitHub **Squash and Merge**

## Strict overall workflow

1. **Create the execution task first.** Use the available task system or the repository's established mechanism. If unavailable, disclose that; do not pretend tasks exist. Follow [task management](references/task-management.md).
2. **Inspect the actual repository.** Record branch and commit; trace real user flows and implementation. Do not infer bugs from assumptions. Follow [investigation](references/investigation.md).
3. **Qualify and deduplicate.** Search open/closed GitHub issues and PRs. Create no more than two new issues, and document each confirmed new bug before editing its code. Follow [issue management](references/issues.md).
4. **Create/link a bug task.** Track each accepted bug independently. Do not create duplicate tasks.
5. **Implement only safe fixes.** Fix no more than two bugs. Keep changes focused, inspect the complete diff, and run relevant available verification. Defer uncertain or risky work. Follow [implementation](references/implementation.md).
6. **Prepare the daily branch.** Generate today's `dd-mm-yyyy` name using `Asia/Dhaka`. Reuse an existing remote branch without resetting it; otherwise create it from the latest `main`. Create one feature branch per bug from the daily branch.
7. **Open dedicated PRs.** Open at most two PRs, each linked to its issue and targeting the daily branch. Never target `main`. Follow [branch and PR rules](references/branches-and-prs.md).
8. **Manage the PR lifecycle.** Monitor required Actions/checks, approvals, and conversations. Repair understood failures and resolve only clearly understood conflicts. Never bypass protection. Squash-merge only when all conditions are satisfied. Follow [PR lifecycle](references/pr-lifecycle.md).
9. **Verify, then clean up.** Confirm the PR is merged, record the merge SHA, and verify its changes are in the daily branch. Only then delete that PR's exact remote feature branch and verify deletion. Never delete `main` or the daily branch.
10. **Synchronize tasks and report facts.** Mark work done only after the full required lifecycle is verified. Report pending, blocked, failed, and deferred work accurately. Follow [execution report](references/execution-report.md).

## Non-negotiable safeguards

- Never report an action as successful without checking its actual result.
- Never fabricate reproduction, source evidence, verification, CI status, approval, merge SHA, or branch deletion.
- Never reset, recreate, overwrite, or force-push the daily integration branch.
- Never automatically merge the daily branch into `main`.
- Never bypass branch protection, required checks, reviews, approvals, or unresolved blocking conversations.
- Never disable or weaken workflows to obtain a passing result.
- Never blindly choose one side of a conflict or discard unrelated work.
- Never delete an unmerged feature branch, another execution's in-progress branch, `main`, or the daily branch.
- Never close issues or mark tasks complete prematurely.
- If the task system, GitHub capability, credentials, or required verification is unavailable, state the limitation and leave affected work open or blocked.

## Completion definition

A bug lifecycle is complete only after its focused implementation has been reviewed, relevant verification has succeeded, its dedicated PR has been confirmed squash-merged into the correct daily branch, the merge commit and integrated changes have been verified, and the exact remote feature branch has been confirmed deleted. If any stage is incomplete, report the completed stages and preserve the remaining work as pending, blocked, or deferred.

## Reference material

Load the relevant reference files as each stage is reached. Read the PR lifecycle and execution-report references before managing or reporting PR outcomes. Read the FAQ when an edge case or operational question arises.

- [Task management](references/task-management.md)
- [Repository investigation](references/investigation.md)
- [GitHub issue management](references/issues.md)
- [Implementation and verification](references/implementation.md)
- [Daily branches and pull requests](references/branches-and-prs.md)
- [PR lifecycle, CI, conflicts, merge, and cleanup](references/pr-lifecycle.md)
- [Execution report](references/execution-report.md)

## FAQ

FAQ content is intentionally not answered in this entrypoint. See [references/faq.md](references/faq.md).
