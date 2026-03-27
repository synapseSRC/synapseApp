import Foundation
import shared

class AuthViewModel: ObservableObject {
    private let analyticsService: AnalyticsService

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    init(analyticsService: AnalyticsService) {
        self.analyticsService = analyticsService
    }

    func login(navigator: AppNavigator) {
        isLoading = true
        errorMessage = nil

        analyticsService.trackEvent("login_attempt", parameters: nil)

        // Simulate network request
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            guard let self = self else { return }
            self.isLoading = false
            self.analyticsService.trackEvent("login_success", parameters: nil)

            navigator.isUserLoggedIn = true
            navigator.reset()
        }
    }

    func register(navigator: AppNavigator) {
        isLoading = true
        errorMessage = nil

        analyticsService.trackEvent("register_attempt", parameters: nil)

        // Simulate network request
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            guard let self = self else { return }
            self.isLoading = false
            self.analyticsService.trackEvent("register_success", parameters: nil)

            navigator.isUserLoggedIn = true
            navigator.reset()
        }
    }
}
