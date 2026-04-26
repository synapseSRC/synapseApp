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

    private init() {
        self.analyticsService = DefaultAnalyticsService()
        self.crashReportingService = DefaultCrashReportingService()

        // Initialize KMP Shared Auth Use Cases
        let client = SupabaseClient.shared.client
        self.authRepository = SupabaseAuthRepository(client: client)
        self.signInUseCase = SignInUseCase(repository: self.authRepository)
        self.signUpUseCase = SignUpUseCase(repository: self.authRepository)
        self.sendPasswordResetUseCase = SendPasswordResetUseCase(repository: self.authRepository)
    }

    // ViewModels
    @MainActor
    func makeAuthViewModel() -> AuthViewModel {
        let viewModel = AuthViewModel(analyticsService: analyticsService)
        viewModel.setDependencies(
            signInUseCase: signInUseCase,
            signUpUseCase: signUpUseCase,
            sendPasswordResetUseCase: sendPasswordResetUseCase
        )
        return viewModel
    }

    func makeHomeViewModel() -> HomeViewModel {
        return HomeViewModel(analyticsService: analyticsService)
    }
}
