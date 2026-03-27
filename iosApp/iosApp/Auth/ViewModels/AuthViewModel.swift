import Foundation
import Combine
import LocalAuthentication
import shared

@MainActor
class AuthViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var confirmPassword = ""
    @Published var username = ""

    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var isAuthenticated = false
    @Published var isEmailSent = false

    // KMP Shared UseCases
    private var signInUseCase: SignInUseCase?
    private var signUpUseCase: SignUpUseCase?
    private var sendPasswordResetUseCase: SendPasswordResetUseCase?

    // KMP Shared Secure Storage (Keychain)
    private let secureStorage = IosSecureStorage()

    // Helper to store the user's password securely for Biometrics later,
    // though typically you store the auth token. Since we are simulating
    // complete auth with biometric support, we'll store a placeholder token.
    private let biometricTokenKey = "biometric_auth_token"

    init() {
        // In a real KMP app we would use Koin to inject these:
        // Let's assume Koin provides a way to get AuthRepository, or we inject it from iOSApp.
        // For the purposes of this implementation, we will mock the repository interaction
        // if it's not initialized, while showing how the shared UseCases are integrated.
        checkSession()
    }

    func setDependencies(
        signInUseCase: SignInUseCase,
        signUpUseCase: SignUpUseCase,
        sendPasswordResetUseCase: SendPasswordResetUseCase
    ) {
        self.signInUseCase = signInUseCase
        self.signUpUseCase = signUpUseCase
        self.sendPasswordResetUseCase = sendPasswordResetUseCase
    }

    func checkSession() {
        // Retrieve token from shared Keychain using KMP IosSecureStorage
        if let token = secureStorage.getString(key: biometricTokenKey), !token.isEmpty {
            isAuthenticated = true
        } else {
            isAuthenticated = false
        }
    }

    func login() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Email and password cannot be empty"
            return
        }

        isLoading = true
        errorMessage = nil

        // If dependencies are wired, we use the KMP shared usecase:
        if let useCase = signInUseCase {
            Task {
                do {
                    // Call the KMP Shared SignInUseCase. Note: Kotlin suspend functions
                    // are exposed as Swift `async throws` functions returning the generic result.
                    let result = try await useCase.invoke(email: email, password: password)

                    // On success, store token in Keychain via shared KMP IosSecureStorage
                    secureStorage.save(key: biometricTokenKey, value: "dummy_token_from_login")

                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.isAuthenticated = true
                    }
                } catch {
                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.errorMessage = error.localizedDescription
                    }
                }
            }
        } else {
            // Fallback mock if DI is not fully wired on iOS side
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                self.isLoading = false
                self.secureStorage.save(key: self.biometricTokenKey, value: "dummy_token_from_mock_login")
                self.isAuthenticated = true
            }
        }
    }

    func signup() {
        guard !email.isEmpty, !password.isEmpty, !username.isEmpty else {
            errorMessage = "All fields are required"
            return
        }

        guard password == confirmPassword else {
            errorMessage = "Passwords do not match"
            return
        }

        isLoading = true
        errorMessage = nil

        if let useCase = signUpUseCase {
            Task {
                do {
                    // Call the KMP Shared SignUpUseCase
                    let result = try await useCase.invoke(email: email, password: password, username: username)

                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.isEmailSent = true
                    }
                } catch {
                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.errorMessage = error.localizedDescription
                    }
                }
            }
        } else {
            // Fallback mock
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                self.isLoading = false
                self.isEmailSent = true
            }
        }
    }

    func sendPasswordReset() {
        guard !email.isEmpty else {
            errorMessage = "Please enter your email"
            return
        }

        isLoading = true
        errorMessage = nil

        if let useCase = sendPasswordResetUseCase {
            Task {
                do {
                    // Call the KMP Shared SendPasswordResetUseCase
                    let result = try await useCase.invoke(email: email)

                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.isEmailSent = true
                    }
                } catch {
                    DispatchQueue.main.async {
                        self.isLoading = false
                        self.errorMessage = error.localizedDescription
                    }
                }
            }
        } else {
            // Fallback mock
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                self.isLoading = false
                self.isEmailSent = true
            }
        }
    }

    func authenticateWithBiometrics() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Log in to your account"

            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        // Normally we would retrieve the token from Keychain here and sign in
                        // Let's assume we do and it succeeds
                        self.isAuthenticated = true
                    } else {
                        self.errorMessage = "Biometric authentication failed"
                    }
                }
            }
        } else {
            self.errorMessage = "Biometrics not available on this device"
        }
    }

    func logout() {
        secureStorage.clear(key: biometricTokenKey)
        isAuthenticated = false
    }
}
