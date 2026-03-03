---
title: Project Roadmap
description: Future plans for Synapse Social.
---

This document tracks the migration of Synapse Social from a native Android application to a **Universal Engine** architecture.

## Vision
Build a high-performance, memory-safe "Engine" in Kotlin (LLVM) that handles business logic, networking, and E2EE once, powering native luxury UIs for Android, iOS, and Web.

---

## Implementation Phases

### Phase 1: Infrastructure & Scaffolding
- [x] Create `:shared` module.
- [x] Configure targets: `android`, `ios`, `wasmJs`.
- [x] Implement cross-platform secrets.
- [x] Setup `Koin` for shared dependency injection.

### Phase 2: Identity & Authentication
- [x] Migrate DTOs and Domain Models to `commonMain`.
- [x] Move `SupabaseClient` and Auth Logic to `shared`.
- [x] Implement Multiplatform `TokenManager`.
- [ ] Connect Android UI (Compose) to Shared Auth Repository.

### Phase 3: Data Persistence & Encryption
- [x] Refactor Room Database to `commonMain`.
- [ ] Enable KSP for Room KMP.
- [ ] Implement Encrypted SQLite drivers.
- [ ] **E2EE Identity:** Generate/Store device-specific identity keys.

### Phase 4: Multi-Platform UI Implementation
- [ ] **Android:** Maintain current Compose UI.
- [x] **iOS:** Scaffold native **SwiftUI** project linked to `:shared`.
- [ ] **Web:** Scaffold **React/Wasm** project linked to `:shared`.

---

## E2EE Strategy (Signal-Inspired)
- **Protocol:** Signal-style Double Ratchet.
- **Keys:** X25519 for handshakes, AES-GCM for payloads.
- **Group Logic:** Server-side fan-out of encrypted keys (Sender Keys).
- **Media:** Encryption of byte-streams before upload.
