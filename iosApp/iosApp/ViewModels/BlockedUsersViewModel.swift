import Foundation
import shared

@MainActor
class BlockedUsersViewModel: ObservableObject {
    @Published var blockedUsers: [BlockedUser] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    private let getBlockedUsersUseCase: GetBlockedUsersUseCase

    init(getBlockedUsersUseCase: GetBlockedUsersUseCase) {
        self.getBlockedUsersUseCase = getBlockedUsersUseCase
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let result = try await getBlockedUsersUseCase.invoke()
            if let users = result.getOrNull() as? [BlockedUser] {
                self.blockedUsers = users
            } else if let error = result.exceptionOrNull() {
                self.errorMessage = error.localizedDescription
            }
        } catch {
            self.errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
