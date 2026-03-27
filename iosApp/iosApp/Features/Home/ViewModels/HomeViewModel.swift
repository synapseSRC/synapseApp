import Foundation
import shared

class HomeViewModel: ObservableObject {
    private let analyticsService: AnalyticsService

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    init(analyticsService: AnalyticsService) {
        self.analyticsService = analyticsService
    }

    func fetchHomeData() {
        isLoading = true
        errorMessage = nil

        analyticsService.trackEvent("home_feed_loaded", parameters: nil)

        // Simulate network request
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            guard let self = self else { return }
            self.isLoading = false
        }
    }
}
