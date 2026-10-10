import Foundation
import shared

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var user: User? = nil
    @Published var isUploading: Bool = false
    @Published var uploadError: String? = nil
    @Published var generalError: String? = nil

    private let iosDependencies = IOSDependencies.shared

    private var currentUid: String {
        return iosDependencies.getAuthRepository().getCurrentUserId() ?? ""
    }

    func loadProfile() async {
        let uid = currentUid
        guard !uid.isEmpty else {
            self.generalError = "No authenticated user ID found"
            return
        }

        do {
            // Swift correctly interprets the Kotlin @Throws wrapper returning User?
            let fetchedUser = try await iosDependencies.fetchUserProfile(uid: uid)
            self.user = fetchedUser
        } catch {
            self.generalError = "Failed to load profile: \(error.localizedDescription)"
        }
    }

    func updateProfile(displayName: String, bio: String) async {
        let uid = currentUid
        guard !uid.isEmpty else {
            self.generalError = "No authenticated user ID found"
            return
        }

        do {
            let updates: [String: Any] = ["display_name": displayName, "bio": bio]
            _ = try await iosDependencies.saveUserProfile(uid: uid, updates: updates)
            await loadProfile()
        } catch {
            self.generalError = "Failed to update profile: \(error.localizedDescription)"
        }
    }

    func uploadProfileImage(data: Data) async {
        let uid = currentUid
        guard !uid.isEmpty else {
            self.uploadError = "No authenticated user ID found"
            return
        }

        self.isUploading = true
        self.uploadError = nil

        do {
            // Convert to JPEG data to ensure format consistency
            guard let image = UIImage(data: data),
                  let jpegData = image.jpegData(compressionQuality: 0.8) else {
                self.uploadError = "Failed to process image data"
                self.isUploading = false
                return
            }

            // For KMP integration we'll simulate saving to a tmp path:
            let tempDir = FileManager.default.temporaryDirectory
            let fileName = UUID().uuidString + ".jpg"
            let tempUrl = tempDir.appendingPathComponent(fileName)

            try jpegData.write(to: tempUrl)

            let uploadUseCase = KMPHelper.sharedHelper.uploadMediaUseCase

            // UploadMediaUseCase returns a Result<String> mapped natively.
            // Since Swift unwraps Kotlin Results, it directly throws on error or returns String
            let resultUrl = try await uploadUseCase.invoke(
                filePath: tempUrl.path,
                mediaType: .photo,
                bucketName: SupabaseClient.shared.BUCKET_USER_AVATARS,
                onProgress: { _ in }
            )

            let updates: [String: Any] = ["avatar_url": resultUrl]
            _ = try await iosDependencies.saveUserProfile(uid: uid, updates: updates)
            await loadProfile()

            // Cleanup tmp file
            try? FileManager.default.removeItem(at: tempUrl)

        } catch {
            self.uploadError = error.localizedDescription
        }

        self.isUploading = false
    }
}
