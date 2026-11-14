import SwiftUI
import shared

struct FollowersFollowingView: View {
    let userId: String
    @State private var selectedTab = 0 // 0 = Followers, 1 = Following
    @State private var users: [shared.User] = []
    @State private var isLoading = false
    @State private var loadError: String? = nil

    private var title: String {
        selectedTab == 0 ? String(localized: "profile_tab_followers") : String(localized: "profile_tab_following")
    }

    private var isFollowers: Bool { selectedTab == 0 }

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text(String(localized: "profile_tab_followers")).tag(0)
                Text(String(localized: "profile_tab_following")).tag(1)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal, AppTheme.Spacing.space16)
            .padding(.top, AppTheme.Spacing.space8)

            Group {
                if isLoading {
                    VStack(spacing: AppTheme.Spacing.space8) {
                        ProgressView()
                        Text("Loading \(title.lowercased())...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let loadError = loadError {
                    VStack(spacing: AppTheme.Spacing.space8) {
                        Text("Unable to load \(title.lowercased())")
                            .font(.headline)
                        Text(loadError)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        Button(action: load) {
                            Text(String(localized: "action_try_again"))
                        }
                    }
                    .padding()
                } else if users.isEmpty {
                    VStack(spacing: AppTheme.Spacing.space8) {
                        Image(systemName: "person.2.slash")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No \(title.lowercased()) yet")
                            .font(.headline)
                        Text("This profile has no \(title.lowercased()) to display.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding()
                } else {
                    List(users, id: \.uid) { user in
                        userRow(user)
                    }
                    .listStyle(.plain)
                }
            }

            Spacer(minLength: 0)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: selectedTab) { _ in load() }
        .task { load() }
    }

    @ViewBuilder
    private func userRow(_ user: shared.User) -> some View {
        HStack(spacing: AppTheme.Spacing.space12) {
            Group {
                if let avatar = user.avatar, !avatar.isEmpty {
                    CachedImageView(urlString: avatar)
                } else {
                    Circle().fill(Color.gray.opacity(0.3))
                }
            }
            .frame(width: 44, height: 44)
            .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(user.username ?? user.displayName ?? "User")
                    .font(.body)
                if let username = user.username, !username.isEmpty {
                    Text("@\(username)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            if user.verify {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundColor(.blue)
                    .accessibilityLabel("Verified")
            }
        }
        .padding(.vertical, AppTheme.Spacing.space4)
    }

    private func load() {
        guard !userId.isEmpty else {
            users = []
            loadError = nil
            isLoading = false
            return
        }
        isLoading = true
        loadError = nil
        let useCase = isFollowers
            ? KMPHelper.sharedHelper.getFollowersUseCase
            : KMPHelper.sharedHelper.getFollowingUseCase
        guard let useCase = useCase else {
            isLoading = false
            loadError = "Follow data is not available."
            return
        }
        Task {
            do {
                let result = try await useCase.invoke(userId: userId)
                if let fetched = result.getOrNull() as? [shared.User] {
                    users = fetched
                } else if let err = result.exceptionOrNull() {
                    loadError = err.message ?? "Unknown error"
                }
                isLoading = false
            } catch {
                loadError = error.localizedDescription
                isLoading = false
            }
        }
    }
}
