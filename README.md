[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](#)
[![Kotlin Version](https://img.shields.io/badge/kotlin-2.3.20-blue.svg)](https://kotlinlang.org/)
[![Compose Version](https://img.shields.io/badge/compose-2026.03.00-orange.svg)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/backend-Supabase-green.svg)](https://supabase.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-informational.svg)](#)

<h1 align="center">⚡ Synapse Social</h1>
<p align="center"><i>Native UI. Shared Logic. Real-time everything.</i></p>

---

Synapse Social is a cross-platform social media application built on the **"Native UI, Shared Logic"** philosophy. Powered by Kotlin Multiplatform, it shares business logic, networking, and data layers across Android and iOS — while each platform delivers a fully native experience via **Jetpack Compose** (Android) and **SwiftUI** (iOS).

The backend runs on **Supabase** — handling real-time messaging, authentication, and file storage at scale.

---

## ✨ Features

| Feature | Description |
|--------|-------------|
| 🧩 **Kotlin Multiplatform** | Shared engine for networking, database caching, and business logic |
| 🎨 **Native UI** | 100% Jetpack Compose on Android, SwiftUI on iOS — no compromises |
| 💬 **Real-time Chat** | Live messaging with Supabase Realtime subscriptions |
| 🗂️ **X-style Threads** | Nested comment threading with inline reply support |
| 👥 **Group Chat** | AI-powered smart replies via Gemini SDK |
| 🎥 **Rich Media** | Posts with images, video (ExoPlayer), and markdown rendering |
| 🔒 **Privacy-First** | Encrypted local storage, secure auth flows |
| 🏗️ **Extensible** | Modular Clean Architecture for long-term maintainability |

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17 or higher**: Ensure your `JAVA_HOME` is set correctly.
- **Android SDK**: Latest stable version (API 35+).
- **Xcode (macOS only)**: For iOS development.
- **Node.js**: For the Web target.

### Configuration
1. Clone the repository.
2. Copy `gradle.properties` (if not pre-configured) and ensure the following keys are set:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   GEMINI_API_KEY=your-gemini-api-key-here
   ```
3. For Android, ensure `google-services.json` is present in the `app/` directory if required for specific Firebase features.

### Running the App
- **Android**: Run `./gradlew :app:installDebug` or use Android Studio.
- **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode and run.
- **Desktop**: Run `./gradlew :desktop:run`.
- **Web**: Run `./gradlew :web:wasmJsBrowserDevelopmentRun`.

---

## 🏗️ Project Structure

The project follows a modular KMP architecture:

- **`:shared`**: Core business logic, networking (Ktor), database (SQLDelight), and domain models.
  - `commonMain`: Shared logic for all platforms.
  - `androidMain`: Android-specific implementations.
  - `iosMain`: iOS-specific implementations.
- **`:app`**: Android-specific UI and features using Jetpack Compose.
- **`:iosApp`**: iOS-specific UI using SwiftUI.
- **`:desktop`**: Desktop-specific target using Compose for Desktop.
- **`:web`**: Web-specific target using Kotlin/Wasm.

---

## 🤝 Contributing

Contributions are welcome!

1. Read **[CONTRIBUTING.md](./CONTRIBUTING.md)** for our workflow and commit standards.
2. Read **[AGENTS.md](./AGENTS.md)** for engineering standards and AI agent roles.
3. Fork and create your branch: `git checkout -b feature/YourFeature`.
4. Commit with a clear message: `git commit -m '✨ feat: Add YourFeature'`.
5. Push and open a Pull Request.

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](./LICENSE) file for details.

---

## 👥 Contact

| Role | Name | Contact |
|------|------|---------|
| Lead Developer | Ashik Ahmed | [iamashik.ms@hotmail.com](mailto:iamashik.ms@hotmail.com) |
| Organization | StudioAs Inc. | @StudioAsInc |

---

<p align="center"><i>Built with ❤️ by the StudioAs Inc. team.</i></p>
