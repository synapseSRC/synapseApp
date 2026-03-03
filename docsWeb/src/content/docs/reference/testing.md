---
title: Testing Strategy
description: Overview of testing practices for Synapse Social.
---

Quality is paramount. We use a mix of Unit, Integration, and UI tests.

## Testing Pyramid

1.  **Unit Tests (70%)**
    - **Scope:** Domain logic, ViewModels, Utility classes, Repositories (mocked).
    - **Location:** `src/test/java` (Android) or `commonTest` (Shared).
    - **Tech:** JUnit 4/5, Mockito/MockK, Kotlin Coroutines Test.

2.  **Integration Tests (20%)**
    - **Scope:** Database queries, API parsing, Repository integrations.
    - **Location:** `src/androidTest/java`.
    - **Tech:** Room Testing, Ktor Mock Engine.

3.  **UI Tests (10%)**
    - **Scope:** Critical user flows (Login, Posting).
    - **Location:** `src/androidTest/java`.
    - **Tech:** Jetpack Compose Testing.

---

## Running Tests

### Unit Tests
Run all local unit tests:
```bash
./gradlew test
```

Run tests for a specific module:
```bash
./gradlew :shared:testDebugUnitTest
./gradlew :app:testDebugUnitTest
```

### Instrumented (UI/Integration) Tests
Requires a connected device or emulator.
```bash
./gradlew connectedAndroidTest
```

---

## Writing Tests

### ViewModel Test Example
```kotlin
@Test
fun `login success updates state`() = runTest {
    // Given
    val viewModel = AuthViewModel(mockUseCase)

    // When
    viewModel.onLogin("user", "pass")

    // Then
    assertEquals(AuthUiState.Success, viewModel.uiState.value)
}
```

### Compose Test Example
```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun myButton_showsText() {
    composeTestRule.setContent {
        MyButton(text = "Click Me")
    }

    composeTestRule.onNodeWithText("Click Me").assertIsDisplayed()
}
```
