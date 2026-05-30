# Contributing to Synapse Social

First off, thank you for considering contributing to Synapse Social! It's people like you that make the open-source community such a great place.

## Code of Conduct

By participating in this project, you agree to abide by our code of conduct. We expect all contributors to be respectful and professional in their interactions.

## Branching Strategy

We follow a simple branching strategy to keep the repository organized:

- **`main`**: The stable branch. All releases are tagged from here.
- **`develop`**: The integration branch for features.
- **`feature/feature-name`**: For new features.
- **`bugfix/issue-description`**: For bug fixes.
- **`hotfix/urgent-fix`**: For urgent fixes to the `main` branch.

### Workflow
1. Fork the repository and create your branch from `develop`.
2. Implement your changes and add tests where applicable.
3. Ensure the build passes and follow the pre-commit checklist in [AGENTS.md](./AGENTS.md).
4. Submit a Pull Request to the `develop` branch.

## Commit Message Format

We use **Conventional Commits** to automate changelog generation and versioning. The commit message should follow this pattern:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools and libraries

### Example
`feat(chat): implement real-time message indicators`

## Pull Request Requirements

Before your PR can be merged, it must meet the following criteria:

1. **Clear Description**: Explain *what* changed and *why*.
2. **Passes CI**: All automated tests and build checks must pass.
3. **Code Style**: Adheres to the engineering standards defined in [AGENTS.md](./AGENTS.md).
4. **Reviews**: At least one approved review from a maintainer.

## Reporting Issues

Use the GitHub issue tracker to report bugs or suggest features. Please provide as much detail as possible, including steps to reproduce for bugs.
