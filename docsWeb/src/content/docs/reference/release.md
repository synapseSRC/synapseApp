---
title: Release Process
description: Versioning and release strategy for Synapse Social.
---

This document outlines the versioning and release strategy for the Android application.

## Versioning
We use semantic versioning (`MAJOR.MINOR.PATCH`) mapped to Android version codes.

- **versionName:** `X.Y.Z` (e.g., `1.0.2`)
- **versionCode:** Integer (e.g., `10002`)

### Logic
- **MAJOR:** Significant rewrites or breaking architectural changes.
- **MINOR:** New features.
- **PATCH:** Bug fixes.

## Build Flavors & Types

Currently, we rely primarily on **Build Types**:

| Type | Description | App ID Suffix |
| :--- | :--- | :--- |
| **Debug** | Development build. Logs enabled. Test environment. | `.debug` |
| **Release** | Production build. Optimized (ProGuard/R8). | (none) |

---

## Release Steps

1.  **Preparation**
    - Update `versionName` and `versionCode` in `app/build.gradle`.
    - Run full regression tests (`./gradlew test connectedAndroidTest`).

2.  **Building the Bundle**
    To build a signed App Bundle for the Play Store:
    ```bash
    ./gradlew bundleRelease
    ```

3.  **Building the APK (Sideloading)**
    ```bash
    ./gradlew assembleRelease
    ```

4.  **Tagging**
    Tag the commit in Git:
    ```bash
    git tag -a v1.x.x -m "Release description"
    git push origin --tags
    ```

---

## Signing
Release builds require a `keystore.properties` file in the root directory (not committed to Git).
