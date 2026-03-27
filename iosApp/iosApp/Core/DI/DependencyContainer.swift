import Foundation
import shared

/// A lightweight Dependency Injection Container
class DependencyContainer: ObservableObject {
    static let shared = DependencyContainer()

    let analyticsService: AnalyticsService
    let crashReportingService: CrashReportingService

    private init() {
        self.analyticsService = DefaultAnalyticsService()
        self.crashReportingService = DefaultCrashReportingService()
    }

    // ViewModels
    func makeAuthViewModel() -> AuthViewModel {
        return AuthViewModel(analyticsService: analyticsService)
    }

    func makeHomeViewModel() -> HomeViewModel {
        return HomeViewModel(analyticsService: analyticsService)
    }
}
