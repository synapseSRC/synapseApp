import SwiftUI

struct HomeView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeHomeViewModel()
    @State private var showPostOptions = false
    @State private var optionsPost: Post?
    @State private var postToShare: Post?

    var body: some View {
        NavigationStack(path: $navigator.homePath) {
            ZStack {
                if viewModel.uiState.isLoading && viewModel.uiState.posts.isEmpty {
                    LoadingView(message: "Loading feed...")
                } else if let errorMessage = viewModel.uiState.errorMessage, viewModel.uiState.posts.isEmpty {
                    ErrorBoundaryView(title: String(localized: "label_error"), message: errorMessage) {
                        viewModel.fetchHomeData()
                    }
                } else {
                    ScrollView {
                        StoriesTrayView()

                        Divider()
                            .background(Color.gray.opacity(0.3))

                        LazyVStack(spacing: AppTheme.Spacing.space0) {
                            ForEach(viewModel.uiState.posts) { post in
                                NavigationLink(value: AppRoute.postDetail(post.id)) {
                                    PostCardView(
                                        post: post,
                                        onLike: { viewModel.toggleLike(for: post) },
                                        onComment: { /* handled by nav */ },
                                        onShare: { postToShare = post },
                                        onBookmark: { viewModel.toggleBookmark(for: post) },
                                        onMoreOptions: {
                                            optionsPost = post
                                            showPostOptions = true
                                        }
                                    )
                                }
                                .buttonStyle(PlainButtonStyle())
                            }

                            if viewModel.uiState.isLoading && !viewModel.uiState.posts.isEmpty {
                                ProgressView()
                                    .padding()
                            }
                        }
                    }
                    .refreshable {
                        viewModel.refresh()
                    }
                }
            }
            .navigationTitle(String(localized: "app_name"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { navigator.navigate(to: .profile(userId: nil), on: .home) }) {
                        Image(systemName: "person.crop.circle")
                            .font(.system(size: 24))
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { navigator.navigate(to: .create, on: .home) }) {
                        Image(systemName: "square.and.pencil")
                            .font(.system(size: 24))
                    }
                }
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .settings:
                    SettingsView()
                case .profile(let userId):
                    ProfileView(userId: userId)
                case .create:
                    CreateView()
                case .postDetail(let postId):
                    PostDetailView(postId: postId)
                case .home, .search, .notifications, .login, .register:
                    EmptyView()
                }
            }
            .confirmationDialog("Post Options", isPresented: $showPostOptions, titleVisibility: .visible) {
                Button("Share") {
                    if let post = optionsPost {
                        postToShare = post
                    }
                }
                Button("Bookmark") {
                    if let post = optionsPost {
                        viewModel.toggleBookmark(for: post)
                    }
                }
                Button("Report", role: .destructive) { /* report post */ }
                Button("Cancel", role: .cancel) { }
            } message: {
                if let post = optionsPost {
                    Text("Actions for post by \(post.displayName ?? post.username ?? "Unknown")")
                }
            }
            .sheet(item: $postToShare) { post in
                ShareLink(
                    item: "https://synapse.app/posts/\(post.id)",
                    message: Text(post.postText ?? ""),
                    subject: Text("Shared post")
                )
            }
            .onAppear {
                viewModel.fetchHomeData()
            }
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
            .environmentObject(AppNavigator())
    }
}
