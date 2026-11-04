import Foundation
import UIKit
import shared

@MainActor
class PasskeysViewModel: ObservableObject {
    @Published var passkeys: [Passkey] = []
    @Published var isLoading = false
    @Published var isAdding = false
    @Published var errorMessage: String? = nil

    // KMP Shared UseCases
    private let loadPasskeysUseCase: LoadPasskeysUseCase
    private let addPasskeyUseCase: AddPasskeyUseCase
    private let removePasskeyUseCase: RemovePasskeyUseCase

    init(
        loadPasskeysUseCase: LoadPasskeysUseCase,
        addPasskeyUseCase: AddPasskeyUseCase,
        removePasskeyUseCase: RemovePasskeyUseCase
    ) {
        self.loadPasskeysUseCase = loadPasskeysUseCase
        self.addPasskeyUseCase = addPasskeyUseCase
        self.removePasskeyUseCase = removePasskeyUseCase
    }

    func load() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                // Call the KMP Shared LoadPasskeysUseCase. Note: Kotlin suspend functions
                // are exposed as Swift `async throws` functions returning the generic result.
                let result = try await loadPasskeysUseCase.invoke()

                // The use case returns a Kotlin Result<List<Passkey>>; a failed Result
                // surfaces via exceptionOrNull() rather than a thrown Swift error.
                if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message ?? "Unknown error"
                    self.isLoading = false
                    return
                }

                self.passkeys = result.getOrNull() as? [Passkey] ?? []
                self.isLoading = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func add(deviceName: String = UIDevice.current.model) {
        guard !isAdding else { return }
        isAdding = true
        errorMessage = nil

        Task {
            do {
                // Call the KMP Shared AddPasskeyUseCase.
                let result = try await addPasskeyUseCase.invoke(deviceName: deviceName)

                // The use case returns a Kotlin Result<Unit>; a failed Result surfaces
                // via exceptionOrNull() rather than a thrown Swift error.
                if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message ?? "Unknown error"
                    self.isAdding = false
                    return
                }

                self.isAdding = false
                self.load()
            } catch {
                self.errorMessage = error.localizedDescription
                self.isAdding = false
            }
        }
    }

    func remove(id: String) {
        errorMessage = nil

        Task {
            do {
                // Call the KMP Shared RemovePasskeyUseCase.
                let result = try await removePasskeyUseCase.invoke(id: id)

                // The use case returns a Kotlin Result<Unit>; a failed Result surfaces
                // via exceptionOrNull() rather than a thrown Swift error.
                if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message ?? "Unknown error"
                    return
                }

                self.passkeys.removeAll { $0.id == id }
            } catch {
                self.errorMessage = error.localizedDescription
            }
        }
    }

    func clearError() {
        errorMessage = nil
    }
}
