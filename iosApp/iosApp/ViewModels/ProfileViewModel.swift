import Foundation
import shared

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var user: User? = nil

    private let iosDependencies = IOSDependencies.shared

    // Mock UID for demo since auth isn't wired in iOS yet
    private let currentUid = "dummy-uid"

    func loadProfile() async {
        do {
            // Swift correctly interprets the Kotlin @Throws wrapper returning User?
            let fetchedUser = try await iosDependencies.fetchUserProfile(uid: currentUid)
            self.user = fetchedUser
            print("Called loadProfile")
        } catch {
            print("Error: \(error)")
        }
    }

    func updateProfile(displayName: String, bio: String) async {
        do {
            let updates: [String: Any] = ["display_name": displayName, "bio": bio]
            _ = try await iosDependencies.saveUserProfile(uid: currentUid, updates: updates)
            await loadProfile()
        } catch {
            print("Error: \(error)")
        }
    }
}
