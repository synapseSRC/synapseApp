import SwiftUI

struct HomeFeedView: View {
    @StateObject private var viewModel = FeedViewModel()

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading && viewModel.posts.isEmpty {
                    ProgressView("Loading Feed...")
                        .progressViewStyle(CircularProgressViewStyle())
                } else if let error = viewModel.error {
                    VStack {
                        Text("Failed to load feed")
                            .font(.headline)
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.red)
                        Button("Retry") {
                            viewModel.fetchPosts()
                        }
                        .padding()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(viewModel.posts) { post in
                                NavigationLink(destination: PostDetailView(post: post)) {
                                    PostCardView(
                                        post: post,
                                        onLike: { viewModel.toggleLike(for: post) },
                                        onComment: { /* Handled by navigation */ },
                                        onShare: { /* Share action */ },
                                        onBookmark: { viewModel.toggleBookmark(for: post) }
                                    )
                                }
                                .buttonStyle(PlainButtonStyle())
                            }

                            if viewModel.isLoading && !viewModel.posts.isEmpty {
                                ProgressView()
                                    .padding()
                            } else {
                                Color.clear
                                    .onAppear {
                                        viewModel.loadMore()
                                    }
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
                    Button(action: { /* Profile action */ }) {
                        Image(systemName: "person.crop.circle")
                            .font(.system(size: 24))
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { /* Compose post action */ }) {
                        Image(systemName: "square.and.pencil")
                            .font(.system(size: 24))
                    }
                }
            }
        }
    }
}

struct HomeFeedView_Previews: PreviewProvider {
    static var previews: some View {
        HomeFeedView()
    }
}
