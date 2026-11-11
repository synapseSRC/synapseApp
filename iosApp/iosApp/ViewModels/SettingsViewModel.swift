import Foundation
import shared
import OSLog

@MainActor
class SettingsViewModel: ObservableObject {
    @Published var securityNotificationsEnabled: Bool = true
    private let preferencesRepo = IOSDependencies.shared.getUserPreferencesRepository()
    private let crashReporter: CrashReportingService
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "SettingsViewModel")
    
    private var currentUid: String {
        return IOSDependencies.shared.getAuthRepository().getCurrentUserId() ?? ""
    }

    init(crashReporter: CrashReportingService = DependencyContainer.shared.crashReportingService) {
        self.crashReporter = crashReporter
    }

    func savePreferences() async {
        do {
            _ = try await preferencesRepo.setSecurityNotificationsEnabled(userId: currentUid, enabled: securityNotificationsEnabled)
        } catch {
            logger.error("Error: \(error)")
            crashReporter.recordError(error)
        }
    }
}
