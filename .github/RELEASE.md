# Release Guide

This document defines the conventions for Synapse Social releases: release notes structure,
tag naming, and release title naming. Follow this guide for every release.

---

## Release Process

1. Ensure `main` is at the desired commit.
2. Create and push a release tag (see [Tag Naming](#tag-naming)).
3. Open a release PR or use the GitHub release flow with the notes below.
4. Bump `projectVersion` in `gradle.properties` for the *next* development cycle in a
   separate commit (do not include it in the release commit).

---

## Tag Naming

Tags follow **Semantic Versioning** with a `v` prefix.

| Component | Format | Example |
| --- | --- | --- |
| Major | `v<major>.0.0` | `v2.0.0` |
| Minor | `v<minor>` patch resets to 0 | `v1.2.0` |
| Patch | `v<patch>` | `v1.0.1` |

Rules:
- Always prefix with `v` (e.g. `v1.1.0`, not `1.1.0`).
- Annotate the tag with a message: `git tag -a v1.1.0 -m "Release v1.1.0"`.
- Push tags explicitly: `git push origin v1.1.0`.

Pre-release tags (optional): append a hyphenated prerelease identifier.
- `v1.2.0-rc.1`, `v1.2.0-beta.2`, `v1.2.0-alpha.1`

---

## Release Title Naming

Keep titles concise and human-readable. Prefer the form:

```
v<version> — <short summary>
```

Examples:
- `v1.1.0 — Liquid glass chat surfaces`
- `v1.0.1 — Crash fixes`

If a short summary is not obvious, fall back to just the version: `v1.1.0`.

---

## Release Notes Structure

Release notes are written in Markdown. Use the following structure for every release.

### Top summary (1–3 lines)

A short paragraph describing what this release is about at a high level.

### `# Highlights` (optional)

Bulleted list of the most notable changes for end users. Use this for major releases.

### `## What's Changed`

A bulleted list of changes grouped by type. Use the same commit-message prefixes
(`feat`, `fix`, `perf`, `chore`, `docs`, `refactor`, `test`) used across the repo.

#### Grouping

- `✨ Features`
- `🐞 Fixes`
- `⚡ Performance`
- `📚 Docs`
- `🔧 Chores / Maintenance`
- `♻️ Refactors`
- `🧪 Tests`

Each bullet should be a single sentence, user-facing where possible, and reference the
PR or issue when known: `- <description> (#123)`.

### `## Breaking Changes` (only when applicable)

List any behavior changes that require user or developer action. If there are none,
omit this section entirely.

### `## Full Changelog`

Standard footer generated from Git:

```
## Full Changelog

https://github.com/<owner>/synapse-social/compare/v<prev>...v<new>
```

Replace `<owner>`, `<prev>`, and `<new>` with real values.

### `## Upgrade Notes` (only when applicable)

Step-by-step instructions for upgrading, including required config changes,
environment variable updates, or migration steps.

---

## Conventional Commit Prefixes

Use these prefixes when writing commit messages; they map directly into the
`What's Changed` section above.

| Prefix | Meaning |
| --- | --- |
| `✨ feat:` | New feature |
| `🐞 fix:` | Bug fix |
| `⚡ perf:` | Performance improvement |
| `📚 docs:` | Documentation only |
| `🔧 chore:` | Maintenance, version bumps |
| `♻️ refactor:` | Code change without behavior change |
| `🧪 test:` | Adding or updating tests |
| `💡 style:` | Formatting, naming |
| `build:` / `ci:` | Build system or CI |
| `revert:` | Reverting a previous commit |

---

## Pre-Release Checklist

- [ ] `./gradlew compileDebugKotlin` passes
- [ ] `projectVersion` in `gradle.properties` matches the released version
- [ ] Release notes follow this structure
- [ ] Tag follows `v<major>.<minor>.<patch>` convention
- [ ] Release title follows `v<version> — <short summary>`
- [ ] CHANGELOG (if any) is updated
- [ ] No secrets committed