import Foundation
import shared

/// A lightweight Dependency Injection Container
class DependencyContainer: ObservableObject {
    static let shared = DependencyContainer()

    let analyticsService: AnalyticsService
    let crashReportingService: CrashReportingService

    // Auth Repos & Use Cases
    let authRepository: AuthRepository
    let signInUseCase: SignInUseCase
    let signUpUseCase: SignUpUseCase
    let sendPasswordResetUseCase: SendPasswordResetUseCase

    // Passkey Repos & Use Cases
    let passkeyRepository: PasskeyRepository
    let passkeyPlatformManager: PasskeyPlatformManager
    let addPasskeyUseCase: AddPasskeyUseCase
    let loadPasskeysUseCase: LoadPasskeysUseCase
    let removePasskeyUseCase: RemovePasskeyUseCase
    let signInWithPasskeyUseCase: SignInWithPasskeyUseCase

    private init() {
        self.analyticsService = DefaultAnalyticsService()
        self.crashReportingService = DefaultCrashReportingService()

        // Initialize KMP Shared Auth Use Cases
        let client = SupabaseClient.shared.client
        self.authRepository = SupabaseAuthRepository(client: client)
        self.signInUseCase = SignInUseCase(repository: self.authRepository)
        self.signUpUseCase = SignUpUseCase(repository: self.authRepository)
        self.sendPasswordResetUseCase = SendPasswordResetUseCase(repository: self.authRepository)

        // Initialize KMP Shared Passkey Dependencies
        self.passkeyRepository = PasskeyRepositoryImpl(client: client)
        self.passkeyPlatformManager = IosPasskeyPlatformManager()
        self.addPasskeyUseCase = AddPasskeyUseCase(
            passkeyRepository: self.passkeyRepository,
            passkeyPlatformManager: self.passkeyPlatformManager
        )
        self.loadPasskeysUseCase = LoadPasskeysUseCase(
            passkeyRepository: self.passkeyRepository
        )
        self.removePasskeyUseCase = RemovePasskeyUseCase(
            passkeyRepository: self.passkeyRepository
        )
        self.signInWithPasskeyUseCase = SignInWithPasskeyUseCase(
            authRepository: self.authRepository,
            passkeyRepository: self.passkeyRepository,
            passkeyPlatformManager: self.passkeyPlatformManager
        )
    }

    // ViewModels
    @MainActor
    func makeAuthViewModel() -> AuthViewModel {
        let viewModel = AuthViewModel(analyticsService: analyticsService)
        viewModel.setDependencies(
            signInUseCase: signInUseCase,
            signUpUseCase: signUpUseCase,
            sendPasswordResetUseCase: sendPasswordResetUseCase,
            signInWithPasskeyUseCase: signInWithPasskeyUseCase
        )
        return viewModel
    }

    @MainActor
    func makePasskeysViewModel() -> PasskeysViewModel {
        return PasskeysViewModel(
            loadPasskeysUseCase: loadPasskeysUseCase,
            addPasskeyUseCase: addPasskeyUseCase,
            removePasskeyUseCase: removePasskeyUseCase
        )
    }

    func makeHomeViewModel() -> HomeViewModel {
        return HomeViewModel(analyticsService: analyticsService)
    }

    func makeNotificationsViewModel() -> NotificationsViewModel {
        return NotificationsViewModel()
    }
}
