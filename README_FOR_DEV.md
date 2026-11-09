# Synapse Social

Synapse Social is a Kotlin Multiplatform social media application built for Android, iOS, desktop, and web. It follows a clean architecture approach, uses shared business logic across platforms, and ships with modern UI experiences, secure local storage, real-time messaging, and a structured developer workflow.

This repository is designed for contributors who want to work on the shared domain/data layer, platform UI, or end-to-end feature development across the app ecosystem.

## Overview

Synapse is a cross-platform social platform built around:
- Kotlin Multiplatform (KMP)
- Jetpack Compose on Android
- SwiftUI on iOS
- Compose for Desktop
- Web target with Kotlin/Wasm
- Supabase for auth, realtime messaging, storage, and backend services
- SQLDelight for local persistence
- Signal-based E2EE patterns and secure local storage
- Firebase / Gemini integration for AI-powered features
- WebRTC / LiveKit for calling and media experiences

## Tech Stack

- Kotlin 2.4.x
- Kotlin Multiplatform
- Gradle
- Jetpack Compose
- Material 3
- Koin / Hilt-based DI patterns
- Supabase
- Ktor
- SQLDelight
- Firebase and Gemini
- WebRTC / LiveKit
- Sentry
- Compose Desktop / Kotlin/Wasm

## Repository Structure

```text
synapseApp/
├── .agents/                  # AI agent conventions and supporting tooling
├── .github/                 # GitHub workflows and PR templates
├── app/                     # Android app module
├── desktop/                 # Desktop app module
├── docs/                    # Project documentation
├── iosApp/                  # iOS app project
├── shared/                  # Shared KMP domain/data logic
├── web/                     # Web landing/site app
├── assets/                  # Visual assets for app branding
├── AGENTS.md                # Engineering and contributor standards
├── README.md                # Project landing page
├── build.gradle             # Root Gradle config
├── gradlew / gradlew.bat    # Gradle wrapper
├── gradle.properties        # Project properties
├── settings.gradle          # Included modules
├── .gitignore
├── LICENSE                  # License for the project
└── REVIEW.md                # Project review / engineering notes