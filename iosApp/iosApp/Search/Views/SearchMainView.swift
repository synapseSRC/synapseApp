import SwiftUI
import shared

struct SearchMainView: View {
    @StateObject private var viewModel = SearchViewModel()

    var body: some View {
        NavigationView {
            VStack(spacing: AppTheme.Spacing.space0) {
                // Search Bar Custom Integration (or use standard .searchable if preferred)
                SearchBar(text: $viewModel.searchQuery)
                    .padding(.horizontal)
                    .padding(.bottom, AppTheme.Spacing.space8)

                // Category Picker (only show if searching)
                if !viewModel.searchQuery.isEmpty {
                    Picker("Category", selection: $viewModel.selectedCategory) {
                        ForEach(SearchCategory.allCases) { category in
                            Text(category.rawValue).tag(category)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal)
                    .padding(.bottom, AppTheme.Spacing.space8)
                }

                Divider()

                // Content Area
                ZStack {
                    if viewModel.isLoading {
                        ProgressView("Searching...")
                            .progressViewStyle(CircularProgressViewStyle())
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if let error = viewModel.errorMessage {
                        VStack(spacing: AppTheme.Spacing.space12) {
                            Image(systemName: "exclamationmark.triangle")
                                .font(.system(size: 40))
                                .foregroundColor(.red)
                            Text("Oops! Something went wrong.")
                                .font(.headline)
                            Text(error)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if viewModel.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        // Empty Query -> Discovery Feed
                        DiscoveryFeedView(viewModel: viewModel)
                    } else {
                        // Results View
                        ScrollView {
                            LazyVStack(spacing: AppTheme.Spacing.space0) {
                                switch viewModel.selectedCategory {
                                case .users:
                                    if viewModel.userResults.isEmpty {
                                        EmptyStateView(icon: "person.slash", title: "No Users Found", message: "Try searching for a different handle or name.")
                                    } else {
                                        ForEach(viewModel.userResults, id: \.id) { user in
                                            UserResultRow(user: user)
                                            Divider()
                                        }
                                    }
                                case .posts:
                                    if viewModel.postResults.isEmpty {
                                        EmptyStateView(icon: "doc.text.magnifyingglass", title: "No Posts Found", message: "Try adjusting your search terms.")
                                    } else {
                                        ForEach(viewModel.postResults, id: \.id) { post in
                                            PostResultRow(post: post)
                                            Divider()
                                        }
                                    }
                                case .hashtags:
                                    if viewModel.hashtagResults.isEmpty {
                                        EmptyStateView(icon: "number", title: "No Hashtags Found", message: "Try another #tag.")
                                    } else {
                                        ForEach(viewModel.hashtagResults, id: \.id) { hashtag in
                                            HashtagResultRow(hashtag: hashtag, isTrending: false)
                                            Divider()
                                        }
                                    }
                                case .news:
                                    EmptyStateView(icon: "newspaper", title: "Coming Soon", message: "News search is not yet implemented.")
                                }
                            }
                        }
                        .background(Color(.systemGroupedBackground))
                    }
                }
            }
            .navigationTitle("Search & Explore")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                viewModel.loadDiscoveryContent()
            }
        }
    }
}

// Custom Search Bar for better control than standard .searchable on older iOS versions
struct SearchBar: View {
    @Binding var text: String
    @State private var isEditing = false

    var body: some View {
        HStack {
            TextField("Search Synapse...", text: $text)
                .padding(7) // component-specific search-field inset; kept outside the foundation scale
                .padding(.horizontal, 25) // component-specific wide-field inset
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .overlay(
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                            .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
                            .padding(.leading, AppTheme.Spacing.space8)

                        if isEditing && !text.isEmpty {
                            Button(action: {
                                self.text = ""
                            }) {
                                Image(systemName: "multiply.circle.fill")
                                    .foregroundColor(.gray)
                                    .padding(.trailing, AppTheme.Spacing.space8)
                            }
                        }
                    }
                )
                .padding(.horizontal, AppTheme.Spacing.space10)
                .onTapGesture {
                    self.isEditing = true
                }

            if isEditing {
                Button(action: {
                    self.isEditing = false
                    self.text = ""

                    // Dismiss keyboard
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }) {
                    Text("Cancel")
                }
                .padding(.trailing, AppTheme.Spacing.space10)
                .transition(.move(edge: .trailing))
                .animation(.default)
            }
        }
    }
}

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: AppTheme.Spacing.space16) {
            Spacer().frame(height: 60)
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(.gray)
            Text(title)
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, AppTheme.Spacing.space40)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}
