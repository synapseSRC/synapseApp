# Synapse Social

**Synapse Social** is a high-performance, native social media client built on **Kotlin Multiplatform (KMP)**. It provides a native mobile experience for both Android and iOS while sharing 100% of business logic, data persistence, and security protocols.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Kotlin Version](https://img.shields.io/badge/kotlin-2.3.20-blue.svg)](https://kotlinlang.org/)
[![Compose Version](https://img.shields.io/badge/compose-2026.03.00-orange.svg)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/backend-Supabase-green.svg)](https://supabase.com/)

---

## 🚀 Overview

Synapse Social is designed with a "Native UI, Shared Logic" philosophy. By leveraging Kotlin Multiplatform, the project ensures consistent behavior across platforms without compromising the look and feel that users expect from native Android (Jetpack Compose) and iOS (SwiftUI) applications.

The backend is powered by **Supabase**, providing real-time database capabilities, secure authentication, and scalable file storage.

---

## ✨ Features

- **Kotlin Multiplatform (KMP):** Shared engine for networking, database, and business logic.
- **Native UI:** 100% Jetpack Compose for Android and SwiftUI for iOS.
- **Real-time Chat:** Seamless, live messaging experience using Supabase Realtime.
- **Rich Media Support:** Interactive posts with support for images, videos, and markdown.
- **Privacy-First:** Secure data handling and encrypted local storage.
- **Extensible Architecture:** Clean Architecture and MVVM principles for long-term maintainability.

---

## 🛠️ Tech Stack

### Shared Engine (`:shared`)
- **Networking:** [Ktor](https://ktor.io/)
- **Backend-as-a-Service:** [Supabase-kt](https://github.com/jan-tennert/supabase-kt)
- **Local Database:** [SQLDelight](https://cashapp.github.io/sqldelight/)
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Serialization:** [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Logging:** [Napier](https://github.com/aakira/Napier)

### Android Application (`:app`)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Video Player:** [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3)
- **Notifications:** [OneSignal](https://onesignal.com/)

### iOS Application (`:iosApp`)
- **UI Framework:** [SwiftUI](https://developer.apple.com/xcode/swiftui/)
- **Integration:** Consumes KMP shared logic as a static framework.

---

## 🏛️ Architecture

Synapse Social strictly follows **Clean Architecture** combined with **MVVM (Model-View-ViewModel)**.

```text
Presentation (Compose / SwiftUI)
        ↕ observes StateFlow / Published properties
ViewModel (Presentation state & UI logic)
        ↕ calls
Domain (UseCases / Repository Interfaces / Domain Models)
        ↕ implemented by
Data (Repository Impls / DataSources / DTOs / Mappers)
```

Refer to [**AGENTS.md**](./AGENTS.md) for detailed coding standards, layer boundaries, and project conventions.

---

## 📁 Project Structure

```text
Synapse/
├── app/                  # Android Native UI (Jetpack Compose)
├── shared/               # KMP Shared Engine (Logic, Domain, Data)
├── iosApp/               # iOS Native UI (SwiftUI)
├── gradle/               # Build configuration
└── build.gradle          # Root build script
```

---

## ⚙️ Installation & Setup

### Prerequisites
- **Android Studio Ladybug (2024.2.1)+**
- **JDK 17**
- **Xcode** (For iOS development)

### Quick Start
1.  **Clone the Repository**
    ```bash
    git clone https://github.com/studioasinc/synapse.git
    cd synapse
    ```
2.  **Configure Environment Variables**
    Create or edit `gradle.properties` in the project root:
    ```properties
    SUPABASE_URL=https://your-project.supabase.co
    SUPABASE_ANON_KEY=your-anon-key-here
    GEMINI_API_KEY=your-gemini-api-key-here
    ONESIGNAL_APP_ID=your-onesignal-app-id-here
    ```
3.  **Sync and Build**
    - Open the project in Android Studio.
    - Wait for Gradle sync to complete.
    - Select the `:app` configuration and run on your emulator or device.

---

## 🧪 Testing

We prioritize high test coverage across all layers.

- **Shared Tests:** `./gradlew :shared:test`
- **Android Tests:** `./gradlew :app:test`
- **iOS Tests:** Run via Xcode Test Navigator in `iosApp`.

---

## 🤝 Contributing

We welcome contributions from the community!

1.  Read the **[AGENTS.md](./AGENTS.md)** file to understand our engineering standards.
2.  Fork the repository and create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes using clear, descriptive messages (`git commit -m '✨ feat: Add some AmazingFeature'`).
4.  Open a Pull Request with a clear explanation of your changes.

---

## 📄 License

This project is currently **Unlicensed**.

## 👥 Contact

- **Lead Developer:** Ashik Ahmed ([iamashik.ms@hotmail.com](mailto:iamashik.ms@hotmail.com))
- **Organization:** StudioAs Inc.

---
*Built with ❤️ by the StudioAs Inc. team.*
