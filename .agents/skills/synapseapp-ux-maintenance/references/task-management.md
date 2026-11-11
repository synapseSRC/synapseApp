# Task Management & Execution Tracking

Task creation and maintenance are mandatory. Create the execution-level task **before repository investigation**.

## 1. Choose the task system

Use the task-management integration available to the agent or the repository's established task-management mechanism. Inspect existing project conventions first. If GitHub Issues are the only available tracking mechanism, use clearly distinguishable execution and bug-tracking tasks within the supported conventions. Do not invent an external task system or claim unsupported task operations succeeded.

## 2. Execution task

Create one parent task per run.

**Title:**

`[Autonomous UX] SynapseApp UX Investigation — dd-mm-yyyy`

Include:

- Objective and target repository.
- Execution date and `Asia/Dhaka` timezone.
- Inspected branch and starting commit.
- Limits: 2 new issues, 2 fixes, 2 PRs.
- Daily integration branch name.
- Investigation areas and expected deliverables.
- Links to associated bug issues, bug tasks, and PRs.
- Current state, blockers, and final outcome.

## 3. Applicable subtasks

Create actionable subtasks only for work that applies to the execution:

1. Repository and UI investigation.
2. Candidate qualification and GitHub duplicate detection.
3. Confirmed bug documentation.
4. Root-cause investigation and implementation.
5. Implementation review and verification.
6. Daily integration branch preparation.
7. Feature branch and PR creation.
8. GitHub Actions monitoring and any required correction.
9. Conflict resolution, if needed.
10. Squash merge and merge verification.
11. Feature-branch cleanup.
12. Final report and task synchronization.

Do not create empty, speculative, or meaningless subtasks.

## 4. Bug-specific task

Create one task for each confirmed, previously undocumented bug that is accepted for handling. Link it to the GitHub Issue. Record:

- Bug title and user impact.
- GitHub Issue URL and technical evidence.
- Root cause and intended correction.
- Implementation and verification status.
- Feature branch and PR URL.
- PR base branch, approval/check state, and merge status.
- Squash merge commit SHA, when merged.
- Feature-branch deletion status.
- Any deferral, blocker, or manual decision required.

Each independently fixable bug must have its own task and dedicated PR.

## 5. Task states and transitions

Use the existing task system's equivalent states. Preferred meanings:

| State | Meaning |
|---|---|
| `TODO` | Not started. |
| `IN_PROGRESS` | Actively being investigated or implemented. |
| `REVIEW` | PR or verification is awaiting checks/review. |
| `BLOCKED` | Progress requires an external dependency or manual action. |
| `DEFERRED` | Intentionally not attempted in this execution. |
| `DONE` | All required completion conditions have been verified. |

Update tasks after meaningful transitions. Examples:

- Start investigation → `IN_PROGRESS`.
- Confirm a bug → create/link its bug task and issue.
- Begin implementation → `IN_PROGRESS`.
- Open PR and await checks/reviews → `REVIEW`.
- Encounter an external blocker or unsafe conflict → `BLOCKED`.
- Defer an unsafe or out-of-limit finding → `DEFERRED` with a reason.
- Complete merge, integration verification, and branch deletion → `DONE`.

A PR being opened, a commit being pushed, or CI merely starting is not completion.

## 6. Synchronization and safeguards

- Link parent task, subtasks, bug tasks, issues, and PRs wherever supported.
- Search for and reuse an existing task for the same work when appropriate; do not create duplicates.
- Preserve task history and update existing tasks rather than replacing them unnecessarily.
- Record dependencies, blockers, and ownership/approval requirements where supported.
- Do not close GitHub Issues prematurely.
- Do not mark a bug task `DONE` until its fix is verified, squash-merged into the correct daily branch, the merge is confirmed, and the exact feature branch is confirmed deleted.
- If merge succeeds but branch deletion fails, record the merge as successful and keep the cleanup work `BLOCKED`; do not misreport the overall lifecycle as complete.
- If task management is unavailable, continue the safe repository workflow, use supported GitHub records as the source of truth, and state clearly that task creation/update was unavailable.
