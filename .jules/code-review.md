# Code Review - Eliminate Hardcoding

## Summary
This PR eliminates hardcoded strings, dimensions (.dp), and content descriptions across several feature modules (Search, Profile, Inbox, Crash) by replacing them with string resources and design system tokens (Spacing, Sizes).

## Changes
- **Resources**: Added missing strings for CrashActivity, Contacts search, and Verified badge.
- **Search**:
    - `HashtagCard.kt`: Replaced `80.dp` with `Sizes.WidthLarge`, used `common_hashtag_format` and `common_people_talking_count`.
    - `AccountCard.kt`: Replaced followers count, handle format, and verified badge content description with resources.
    - `SearchScreen.kt`: Replaced hardcoded hashtag labels with resources.
- **Crash**:
    - `CrashActivity.kt`: Externalized all UI strings and replaced `300.dp` height with `Sizes.HeightPreview`.
- **Hashtag**:
    - `HashtagFeedScreen.kt`: Externalized title hashtag format.
- **Inbox**:
    - `ContactsTabScreen.kt`: Externalized search placeholder and content description.
- **Profile**:
    - `CoverPhoto.kt`: Replaced `0.dp` with `Spacing.None`.
    - `ProfileSkeleton.kt`: Replaced `0.dp` with `Spacing.None`.

## Verification Results
- **Compilation**: `./gradlew :app:compileDebugKotlin` passed.
- **Tests**: `./gradlew :app:testDebugUnitTest` passed (no new failures).
