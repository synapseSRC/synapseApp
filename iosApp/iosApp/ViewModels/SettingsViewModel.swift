import Foundation
import shared

@MainActor
class SettingsViewModel: ObservableObject {
    @Published var securityNotificationsEnabled: Bool = true
    private let preferencesRepo = IOSDependencies.shared.getUserPreferencesRepository()
    private let currentUid = "dummy-uid"

    func savePreferences() async {
        do {
            _ = try await preferencesRepo.setSecurityNotificationsEnabled(userId: currentUid, enabled: securityNotificationsEnabled)
        } catch {
            print("Error: \(error)")
        }
    }
}
