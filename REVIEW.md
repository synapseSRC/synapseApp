# Code Review Guidelines

This document outlines the principles and process for conducting code reviews in the Synapse Social project.

## 🎯 Objectives
- **Ensure Code Quality**: Maintain high standards for readability, maintainability, and performance.
- **Identify Bugs**: Catch logic errors, edge cases, and security vulnerabilities early.
- **Knowledge Sharing**: Spread domain knowledge and best practices across the team.
- **Consistency**: Enforce engineering standards defined in [AGENTS.md](./AGENTS.md).

## 🔍 What to Look For

### 🏗️ Architecture
- Does the change follow **Clean Architecture**?
- Are UseCases limited to a single `invoke()`?
- Is there any framework-specific code in the `shared:domain` layer?

### 🧩 KMP Specifics
- Are multiplatform concerns handled correctly via `expect/actual` or interface abstractions?
- Is `commonMain` free of platform-specific imports?

### 🎨 UI (Compose/SwiftUI)
- Is the UI state-driven?
- Are hardcoded values avoided (use `SynapseTheme`, `Spacing`, etc.)?
- Is the presentation logic kept minimal?

### 🧪 Testing
- Are new features accompanied by unit tests?
- Do existing tests still pass?

### 📝 Documentation
- Are public APIs documented with KDoc?
- Is the code self-documenting and easy to follow?

## 🤝 The Review Process

1. **Self-Review**: Always review your own code before opening a PR.
2. **Automated Checks**: Ensure the CI build and tests pass.
3. **Be Constructive**: Provide clear, actionable feedback. Focus on the code, not the person.
4. **Approve or Request Changes**:
   - ✅ **Approve**: If the code meets all standards.
   - 💡 **Comment**: For minor suggestions or questions.
   - ❌ **Request Changes**: If there are blocking issues or architectural violations.

## 🚀 Conventional Comments
Consider using [Conventional Comments](https://conventionalcomments.org/) for clarity:
- `suggestion`: "I suggest renaming this variable for better clarity."
- `nit`: "Minor formatting issue here."
- `question`: "Why was this specific approach chosen over X?"
- `issue`: "This logic will fail if the input is null."
