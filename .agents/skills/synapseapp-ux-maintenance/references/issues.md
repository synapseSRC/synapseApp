# GitHub Issue Management

## 1. Duplicate detection is mandatory

Before creating an issue, search the target repository's GitHub history. Examine:

- Open issues.
- Closed issues.
- Open pull requests.
- Closed and merged pull requests.
- Different descriptions of the same symptom, workflow, or underlying cause.

Compare the underlying defect, not only title wording. If an equivalent issue already exists, do not create another. Link the existing record in the execution task and report. Comment only when there is genuinely useful new technical evidence; otherwise leave it untouched.

## 2. Creation rules

- Only create an issue for a confirmed, evidence-backed, previously undocumented defect.
- Create the GitHub Issue **before changing code for that defect**.
- Maximum 2 newly created issues per execution.
- Use the title format: `[Bug] Concise description of the actual user-facing problem`.
- Apply only labels that already exist in the repository. Never invent labels.
- Assign priority based on actual user impact and existing repository conventions.
- Link the issue to its bug-specific task, if task linking is supported.
- Do not close issues as part of this workflow merely because a fix was committed or a PR was opened.

## 3. Required issue body

Use this structure and fill every section with evidence-backed information:

### Summary
Explain what is broken in plain language.

### User Impact
Describe who is affected, when it happens, and the practical consequence.

### Steps to Reproduce
Provide concrete steps grounded in observed behavior or implementation. Clearly mark steps inferred from code; do not present them as manually reproduced.

### Expected Behavior
Describe what the application should do.

### Actual Behavior
Describe the defective behavior and distinguish observed results from code-based inference.

### Technical Evidence
Include relevant source paths, component/function names, accurate line references where available, and a concise explanation of the execution path or root cause.

### Suggested Fix
Describe a reasonable technical direction without unnecessarily constraining the implementation.

### Acceptance Criteria
List clear, observable conditions that establish resolution.

## 4. After creation

Record the issue URL and number in the execution task and the bug-specific task. Update task state and proceed to safe-fix assessment. If the issue is not eligible for autonomous implementation, leave it open and document the deferral reason in both the issue (when useful) and task.
