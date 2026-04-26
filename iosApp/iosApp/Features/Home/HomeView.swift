import SwiftUI

struct HomeView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeHomeViewModel()

    var body: some View {
        NavigationStack(path: $navigator.homePath) {
            ZStack {
                if viewModel.uiState.isLoading && viewModel.uiState.posts.isEmpty {
                    LoadingView(message: "Loading feed...")
                } else if let errorMessage = viewModel.uiState.errorMessage, viewModel.uiState.posts.isEmpty {
                    ErrorBoundaryView(title: "Error", message: errorMessage) {
                        viewModel.fetchHomeData()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(viewModel.uiState.posts) { post in
                                NavigationLink(value: post) {
                                    PostCardView(
                                        post: post,
                                        onLike: { viewModel.toggleLike(for: post) },
                                        onComment: { /* handled by nav */ },
                                        onShare: { /* share action */ },
                                        onBookmark: { viewModel.toggleBookmark(for: post) }
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
            .navigationTitle("Synapse")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { navigator.navigate(to: .profile, on: .home) }) {
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
                case .profile:
                    ProfileView()
                case .create:
                    CreateView()
                case .home, .search, .notifications, .login, .register:
                    EmptyView()
                }
            }
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
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
