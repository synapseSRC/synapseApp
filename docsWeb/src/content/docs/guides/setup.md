---
title: Setup Guide
description: Learn how to set up your development environment for Synapse Social.
---

Welcome to the team! Follow this guide to set up your development environment for **Synapse Social**.

## Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer (KMP support required).
- **JDK:** Java 17 (Required for Android & Kotlin compatibility).
- **Xcode:** (Optional) Required for running the iOS app.
- **Git:** Latest version.

## Quick Start

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/studioasinc/synapse.git
    cd synapse
    ```

2.  **Open in Android Studio**
    - Select "Open" and choose the root folder.
    - Wait for Gradle Sync to complete.

3.  **Run the Android App**
    - Select the `app` configuration.
    - Choose an Emulator or Physical Device.
    - Click **Run** (Green Play Button).

---

## Environment Variables & Secrets
The app requires several API keys to function (Supabase, Cloudinary, Gemini, etc.). These are injected via `gradle.properties` or Environment Variables.

### Option 1: `gradle.properties` (Local Development)
Create or edit `gradle.properties` in your **user home directory** (`~/.gradle/gradle.properties`) OR the project root (git-ignored) to add:

```properties
# Supabase
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_anon_key

# Supabase Storage (S3)
SUPABASE_SYNAPSE_S3_ENDPOINT_URL=...
SUPABASE_SYNAPSE_S3_REGION=...
SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID=...
SUPABASE_SYNAPSE_S3_ACCESS_KEY=...

# Cloudinary
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...

# AI & Other
GEMINI_API_KEY=...
ONESIGNAL_APP_ID=...
```

:::note
Ask a team lead for the development keys. DO NOT commit real keys to version control.
:::

---

## Build Commands

Run these from the terminal in the project root:

| Task | Command | Description |
| :--- | :--- | :--- |
| **Build Debug APK** | `./gradlew assembleDebug` | Builds the APK. |
| **Run Tests** | `./gradlew test` | Runs unit tests. |
| **Lint Check** | `./gradlew lint` | Runs static analysis. |
| **Clean** | `./gradlew clean` | Deletes build artifacts. |

---

## iOS Setup (Mac Only)
1.  Ensure Xcode is installed.
2.  Open `iosApp/iosApp.xcodeproj` in Xcode.
3.  Wait for the `:shared` module to build/sync.
4.  Run on an iOS Simulator.
