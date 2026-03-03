---
title: Changelog
description: History of changes in the Synapse project.
---

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **Synapse Reels:** Infinite video feed with interactions (likes, opposes, saves).
- **Comments System:** Full support for comments and replies on posts and reels.
- **AI Integration:** Gemini-powered summaries and explanations for chat messages.
- **Notification Center:** Real-time updates for interactions and follows.
- **Settings Hub:** Centralized configuration for appearance, privacy, and account settings.

### Changed
- **KMP Migration:** Shifted core logic to the `:shared` module for cross-platform support.
- **Database:** Transitioned to Room Multiplatform for shared persistence.
- **Architecture:** Implemented Clean Architecture with Unidirectional Data Flow (UDF).

## [0.1.0]

### Added
- Initial scaffolding for Android and iOS projects.
- Shared `:shared` module with Supabase integration.
- Basic Authentication (Email, GitHub, Google).
- User Profiles and Follow system.
- Basic Image and Text posting.

### Fixed
- Various UI bugs in the early Compose implementation.
- Stability improvements for the Supabase client.
