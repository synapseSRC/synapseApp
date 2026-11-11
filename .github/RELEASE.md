# Release Guide

Synapse Social release conventions: release notes structure, tag naming, and release title naming.
Follow this guide for every release.

---

## 1. Title Naming — Fruit Codename Convention

Every release title uses a fruit codename with an emoji. The fruit is tied to the **major** version
line and **never repeats** across major versions.

| Release type | Title format | Example |
| --- | --- | --- |
| Major | `v<major>.0.0 | Fruit Emoji` | `v2.0.0 | Grapes 🍇` |
| Minor | `v<major>.<minor>.0 | Fruit N` | `v2.1.0 | Grapes 2` |
| Patch | `v<major>.<minor>.<patch> | Fruit N` | `v2.1.1 | Grapes 3` |

Rules:

- Fruit name changes **only on major releases**. Within one major line, the same fruit is reused.
- The first release of a major line keeps the plain fruit name (`Grapes 🍇`). Every subsequent
  minor/patch release appends an incrementing number (`Grapes 2`, `Grapes 3`, …).
- Never reuse a fruit name across major versions. Check the registry below before assigning.
- The number always increments on every minor/patch release; it does **not** reset until the next major.

### Current fruit registry

| Major line | Fruit | Emoji | First release | Status |
| --- | --- | --- | --- | --- |
| 1.x | Grapes | 🍇 | v1.1.0 | Active |

### Candidate fruit pool (next major versions)

Pick one unused fruit per new major, then remove it from the pool:

Apple 🍎 · Banana 🍌 · Orange 🍊 · Lemon 🍋 · Lime 🍈 · Mango 🥭 · Pineapple 🍍 · Peach 🍑 ·
Cherry 🍒 · Strawberry 🍓 · Blueberry 🫐 · Raspberry 🫐 · Blackberry 🫐 · Watermelon 🍉 ·
Pear 🍐 · Kiwi 🥝 · Plum 🍑 · Pomegranate 🍹 · Papaya 🍈 · Guava 🍈 · Dragon fruit 🐉 ·
Durian 🍈 · Lychee 🍈 · Coconut 🥥 · Avocado 🥑 · Cantaloupe 🍈 · Honeydew 🍈 · Apricot 🍑 ·
Date 🍇 · Elderberry 🫐 · Passion fruit 🍈 · Starfruit 🍈 · Jackfruit 🍈 · Mangosteen 🍈 ·
Rambutan 🍈 · Sapodilla 🍈 · Cherimoya 🍈 · Mulberry 🫐 · Cranberry 🫐 · Gooseberry 🫐 ·
Cloudberry 🫐 · Lingonberry 🫐 · Huckleberry 🫐 · Bilberry 🫐 · Boysenberry 🫐 ·
Loganberry 🫐 · Tayberry 🫐 · Olive 🫒

### Next-release checker

Before tagging, run this:

```
export LAST_FRUIT="Grapes 🍇"
export LAST_NUMBER=<number of the latest release in this major>
# Major release: pick a NEW fruit from the pool above.
# Minor/patch release: `${LAST_FRUIT} $((LAST_NUMBER + 1))`
```

Example next patch after `v1.1.0 | Grapes 🍇` → `v1.1.1 | Grapes 2`.

---

## 2. Tag Naming

Tags follow **Semantic Versioning** with a `v` prefix.

| Component | Format | Example |
| --- | --- | --- |
| Major | `v<major>.0.0` | `v2.0.0` |
| Minor | `v<major>.<minor>.0` | `v2.1.0` |
| Patch | `v<major>.<minor>.<patch>` | `v2.1.1` |

Rules:

- Always prefix with `v` (e.g. `v1.1.0`, not `1.1.0`).
- Annotate the tag: `git tag -a v2.0.0 -m "Release v2.0.0"`.
- Push tags explicitly: `git push origin v2.0.0`.
- Pre-release tags (optional): append a hyphenated identifier.
  - `v2.0.0-rc.1`, `v2.0.0-beta.2`, `v2.0.0-alpha.1`

---

## 3. Release Notes Structure

Release notes are written in Markdown and rendered identically on Android (`ReleaseNotesParser`)
and the web changelog (`release-notes.ts`). Use this structure for every release.

```markdown
# v<major>.<minor>.<patch> | Fruit Emoji

<one short paragraph describing this release at a high level>

## Highlights

- Bullet summary of the most notable end-user changes (major releases only).

## What's Changed

### Added (Features)

- Description (#pr-number)

### Improved (Improvements)

- Description (#pr-number)

### Fixed (Bug fixes)

- Description (#pr-number)

### Changed

- Description (#pr-number)

## Breaking Changes

<only when applicable — omit otherwise>

## Upgrade Notes

<only when applicable — omit otherwise>

## Full Changelog

https://github.com/synapseSRC/synapseApp/compare/v<prev>...v<new>
```

### Grouping guide

Use the same commit-message prefixes as the repo (`feat`, `fix`, `perf`, `chore`, `docs`,
`refactor`, `test`, `style`, `build`, `ci`, `revert`). Map them to headings:

| Prefix | Release heading |
| --- | --- |
| `feat` / `✨` | Added |
| `fix` / `🐞` | Fixed |
| `perf` / `⚡` | Improved |
| `chore` / `docs` / `refactor` / `style` / `build` / `ci` / `revert` | Changed |
| `test` / `🧪` | Changed (or omit internal-only) |

Each bullet is a single sentence, user-facing where possible, referencing the PR or issue:
`- Liquid glass chat surfaces (#117)`.

Each platform gets its own subsection when the change is platform-specific:

### Added (Features)

- `<description> (#pr)`
- `**Android:** <description> (#pr)`
- `**iOS:** <description> (#pr)`
- `**Desktop (Linux / Windows / macOS):** <description> (#pr)`
- `**Web / PWA:** <description> (#pr)`
- `**Shared core:** <description> (#pr)`

---

## 4. Monorepo / Multiplatform Scope

Synapse Social ships across platforms from one repository. When authoring notes, mark every
change with its target so readers know what's affected:

| Module | Platform | Notes |
| --- | --- | --- |
| `:app` | Android | Jetpack Compose |
| `:iosApp` | iOS | SwiftUI |
| `:desktop` | Linux / Windows / macOS | Compose Desktop (JVM) |
| `:web` | Web / PWA | Next.js |
| `:shared` | All | KMP common core |

---

## 5. Pre-Release Checklist

- [ ] `./gradlew compileDebugKotlin` passes
- [ ] `projectVersion` in `gradle.properties` matches the released version
- [ ] Release notes follow this structure
- [ ] Tag follows `v<major>.<minor>.<patch>` convention
- [ ] Release title follows `v<version> | Fruit Emoji` (fruit never repeated)
- [ ] Fruit assigned from the registry; new major consumes a fresh fruit
- [ ] Each change marked with its platform target
- [ ] CHANGELOG / GitHub release body updated
- [ ] No secrets committed

---

## 6. Conventional Commit Prefixes

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

*Maintained by the Synapse Social team. Update this file when the fruit registry or release
structure changes.*
