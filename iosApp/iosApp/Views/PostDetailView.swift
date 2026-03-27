import SwiftUI

struct PostDetailView: View {
    @StateObject private var viewModel: PostDetailViewModel

    init(post: Post) {
        _viewModel = StateObject(wrappedValue: PostDetailViewModel(post: post))
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    // AI Summary Button for long threads
                    if viewModel.post.commentsCount > 5 {
                        Button(action: {
                            viewModel.summarizeThread()
                        }) {
                            HStack {
                                Image(systemName: "sparkles")
                                Text("AI Thread Summary")
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                        .tint(.purple)
                    }

                    // Full Post Card
                    PostCardView(
                        post: viewModel.post,
                        onLike: { /* Detail like logic */ },
                        onComment: { /* Scroll to comment field */ },
                        onShare: { /* Detail share logic */ },
                        onBookmark: { /* Detail bookmark logic */ }
                    )

                    Divider()
                        .padding(.vertical, 8)

                    // Comments Header
                    HStack {
                        Text("Comments")
                            .font(.headline)
                        Spacer()
                        Text("\(viewModel.post.commentsCount)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)

                    // Comments List
                    if viewModel.isLoading {
                        ProgressView("Loading comments...")
                            .padding()
                    } else if viewModel.comments.isEmpty {
                        Text("No comments yet. Be the first to comment!")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding()
                    } else {
                        LazyVStack(alignment: .leading, spacing: 16) {
                            ForEach(viewModel.comments) { comment in
                                HStack(alignment: .top, spacing: 12) {
                                    VStack(spacing: 0) {
                                        Circle().fill(Color.secondary.opacity(0.3)).frame(width: 40, height: 40)
                                        Rectangle().fill(Color.secondary.opacity(0.5)).frame(width: 2).padding(.top, 4)
                                    }
                                    PostCardView(post: comment, onLike: {}, onComment: {}, onShare: {}, onBookmark: {})
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 20)
                    }
                }
            }

            Divider()

            // Comment Input Area
            HStack {
                TextField("Add a comment...", text: $viewModel.commentText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(viewModel.isSubmittingComment)

                Button(action: {
                    viewModel.submitComment()
                }) {
                    if viewModel.isSubmittingComment {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Image(systemName: "paperplane.fill")
                    }
                }
                .disabled(viewModel.commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isSubmittingComment)
                .padding(10)
                .background(Color.accentColor)
                .foregroundColor(.white)
                .clipShape(Circle())
            }
            .padding()
        }
        .navigationTitle("Post Detail")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $viewModel.showSummarySheet) {
            AIThreadSummarySheet(
                isSummarizing: viewModel.isSummarizing,
                summary: viewModel.summary,
                error: viewModel.summaryError
            )
        }
    }
}
