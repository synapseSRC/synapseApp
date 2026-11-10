import Foundation
import shared
import OSLog

@MainActor
class SettingsViewModel: ObservableObject {
    @Published var securityNotificationsEnabled: Bool = true
    private let preferencesRepo = IOSDependencies.shared.getUserPreferencesRepository()
    private let crashReporter: CrashReportingService
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.synapse.social", category: "SettingsViewModel")

    private var currentUid: String? {
        return DependencyContainer.shared.authRepository.getCurrentUserId()
    }

    init(crashReporter: CrashReportingService = DependencyContainer.shared.crashReportingService) {
        self.crashReporter = crashReporter
        Task { await loadPreferences() }
    }

    func loadPreferences() async {
        guard let userId = currentUid, !userId.isEmpty else {
            logger.warning("Cannot load preferences: no authenticated user ID")
            return
        }
        do {
            let enabled = try await preferencesRepo.getSecurityNotificationsEnabled(userId: userId)
            self.securityNotificationsEnabled = enabled
        } catch {
            logger.error("Error loading preferences: \(error)")
            crashReporter.recordError(error)
        }
    }

    func savePreferences() async {
        guard let userId = currentUid, !userId.isEmpty else {
            logger.warning("Cannot save preferences: no authenticated user ID")
            return
        }
        do {
            _ = try await preferencesRepo.setSecurityNotificationsEnabled(userId: userId, enabled: securityNotificationsEnabled)
        } catch {
            logger.error("Error: \(error)")
            crashReporter.recordError(error)
        }
    }
}
