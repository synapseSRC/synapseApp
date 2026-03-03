---
title: Contributing Guide
description: Learn how to contribute to Synapse Social.
---

Thank you for your interest in contributing to **Synapse Social**! We welcome contributions from developers of all skill levels.

## Code of Conduct
Please be respectful and inclusive. We strive to maintain a welcoming environment for everyone.

---

## Development Workflow

1.  **Find an Issue:** Look for open issues on GitHub. If you want to work on something new, open an issue first to discuss it.
2.  **Fork & Clone:** Fork the repo and clone it locally.
3.  **Branch:** Create a feature branch.
    - Format: `feature/your-feature-name` or `fix/issue-description`
4.  **Code:** Implement your changes.
    - **Adhere strictly** to `AGENT.md` and `CODESTYLE.md`.
    - **Tests:** Add unit tests for logic and UI tests for screens.
5.  **Verify:** Run `./gradlew test` and ensure the app builds.
6.  **Pull Request:** Submit a PR to the `main` branch.
    - Title: Descriptive title (e.g., "Feat: Add Login Screen").
    - Description: Explain what you changed and why. Link the issue.

---

## AI Agents & Tools
If you are an AI agent or using AI tools:
- You **MUST** read and follow `AGENTS.md` in the root directory.
- Explicitly state in your PR description if the code was AI-generated.
- You are responsible for the correctness of the generated code.

---

## Git Commit Messages
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
- `feat: ...` for new features.
- `fix: ...` for bug fixes.
- `docs: ...` for documentation.
- `style: ...` for formatting.
- `refactor: ...` for code restructuring.
- `test: ...` for adding tests.
- `chore: ...` for build/tooling changes.
