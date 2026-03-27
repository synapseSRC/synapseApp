import SwiftUI
import shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let crashReporter: CrashReportingService
    private let analyticsService: AnalyticsService

    init() {
        self.crashReporter = DependencyContainer.shared.crashReportingService
        self.analyticsService = DependencyContainer.shared.analyticsService

        crashReporter.log("App Initialized")
        analyticsService.trackEvent("app_launched", parameters: nil)
    }

    var body: some Scene {
        WindowGroup {
            SplashView()
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                crashReporter.log("App became active")
                analyticsService.trackEvent("app_active", parameters: nil)
            case .inactive:
                crashReporter.log("App became inactive")
            case .background:
                crashReporter.log("App entered background")
                analyticsService.trackEvent("app_backgrounded", parameters: nil)
            @unknown default:
                break
            }
        }
    }
}
