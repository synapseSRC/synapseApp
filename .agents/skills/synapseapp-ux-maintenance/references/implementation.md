# Autonomous Implementation & Verification

## 1. Eligibility gate

After the GitHub Issue has been created, decide whether the defect can be fixed safely in the current execution. Do not treat issue creation as completion.

Defer implementation when any of the following applies:

- Root cause or expected behavior is uncertain.
- The solution requires major architectural or product decisions.
- User data could be lost or corrupted.
- External credentials, infrastructure, or access are unavailable.
- Security-sensitive behavior cannot be safely assessed.
- A product-owner decision or manual approval is necessary.
- The result cannot be adequately reviewed or verified.

Record the reason in the issue/task and mark the task `DEFERRED` or `BLOCKED`, as appropriate.

## 2. Implementation process

For each eligible bug, separately:

1. Re-read the issue, evidence, and relevant implementation.
2. Confirm the root cause before editing.
3. Inspect surrounding components, dependencies, and established abstractions.
4. Choose the smallest dependable correction.
5. Modify only what is needed for this bug.
6. Consider effects on related user flows and shared components.
7. Inspect the complete diff and surrounding code.
8. Record the changed files and rationale in the bug task/PR.

## 3. Implementation principles

- Keep each change focused on one bug.
- Do not combine unrelated fixes in one branch or PR.
- Avoid broad rewrites, unnecessary dependencies, and unrelated refactoring.
- Preserve established architecture, visual design, and interaction conventions.
- Preserve existing features and expected behavior.
- Maintain dark/light theme compatibility and accessibility.
- Prefer existing abstractions over arbitrary hardcoded values.
- Do not hide errors or suppress useful diagnostics merely to make a fix appear successful.
- Do not introduce secrets, credentials, or unrelated configuration changes.

## 4. Review and verification

Review the entire change set before opening the PR. Confirm:

- The change addresses the issue's actual root cause.
- Surrounding implementation remains consistent.
- Existing behavior has not been unintentionally changed.
- Only relevant files are modified.
- No secrets or credentials were introduced.
- Accessibility and theme behavior are preserved where relevant.
- Dependency/configuration changes are necessary and justified.

Use available project commands when appropriate, such as build, lint, formatting, static analysis, and type-checking. Select checks that are relevant to the changed code and the repository's own conventions.

For every command:

- Report the actual result.
- If it fails, inspect the output and determine whether the change caused it or whether it is an unrelated pre-existing/environmental failure.
- Correct understood problems safely.
- Do not make repeated blind changes to force a green result.
- Do not claim successful verification when a command failed, was not run, or could not be accessed.

## 5. Handoff to PR creation

Only after the implementation has been reviewed and relevant available verification has been assessed should the change proceed to a dedicated feature branch and PR. Follow [branches-and-prs.md](branches-and-prs.md) and [pr-lifecycle.md](pr-lifecycle.md).
