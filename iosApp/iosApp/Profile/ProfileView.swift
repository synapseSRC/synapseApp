import SwiftUI

struct ProfileView: View {
    @StateObject private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .frame(width: 100, height: 100)
                        .foregroundColor(.gray)
                        .accessibilityLabel("Profile Picture")

                    Text(viewModel.user?.displayName ?? viewModel.user?.username ?? "Loading...")
                        .font(.title)
                        .fontWeight(.bold)
                        .accessibilityLabel(viewModel.user?.displayName ?? "Loading user details")

                    Text(viewModel.user?.bio ?? "No bio available")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)

                    HStack(spacing: 40) {
                        NavigationLink(destination: FollowersFollowingView()) {
                            VStack {
                                Text("\(viewModel.user?.followersCount ?? 0)").font(.headline)
                                Text("Followers").font(.caption)
                            }
                        }
                        .foregroundColor(.primary)
                        .accessibilityLabel("\(viewModel.user?.followersCount ?? 0) followers")

                        NavigationLink(destination: FollowersFollowingView()) {
                            VStack {
                                Text("\(viewModel.user?.followingCount ?? 0)").font(.headline)
                                Text("Following").font(.caption)
                            }
                        }
                        .foregroundColor(.primary)
                        .accessibilityLabel("\(viewModel.user?.followingCount ?? 0) following")

                        VStack {
                            Text("\(viewModel.user?.postsCount ?? 0)").font(.headline)
                            Text("Posts").font(.caption)
                        }
                        .accessibilityLabel("\(viewModel.user?.postsCount ?? 0) posts")
                    }

                    NavigationLink(destination: EditProfileView(viewModel: viewModel)) {
                        Text("Edit Profile")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding(.horizontal)
                    .accessibilityLabel("Edit your profile")

                    // Highlights/Stories
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(0..<5) { _ in
                                Circle()
                                    .fill(Color.orange.opacity(0.8))
                                    .frame(width: 60, height: 60)
                                    .padding(4)
                            }
                        }
                        .padding(.horizontal)
                    }
                    .accessibilityLabel("User Stories")

                    // Media Grid
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                        ForEach(0..<9) { _ in
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .aspectRatio(1, contentMode: .fit)
                        }
                    }
                }
                .padding(.top)
            }
            .navigationTitle(viewModel.user?.username ?? "Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink(destination: SettingsView()) {
                        Image(systemName: "gear")
                            .accessibilityLabel("Settings")
                    }
                }
            }
            .task {
                await viewModel.loadProfile()
            }
        }
    }
}
