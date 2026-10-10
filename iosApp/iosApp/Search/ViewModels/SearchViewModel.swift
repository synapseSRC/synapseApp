import Foundation
import Combine
import shared

enum SearchCategory: String, CaseIterable, Identifiable {
    case users = "Users"
    case posts = "Posts"
    case hashtags = "Hashtags"
    case news = "News"

    var id: String { self.rawValue }
}

@MainActor
class SearchViewModel: ObservableObject {
    @Published var searchQuery: String = ""
    @Published var selectedCategory: SearchCategory = .users

    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    // Results
    @Published var userResults: [SearchAccount] = []
    @Published var postResults: [SearchPost] = []
    @Published var hashtagResults: [SearchHashtag] = []
    @Published var newsResults: [SearchNews] = []

    // Discovery
    @Published var trendingHashtags: [SearchHashtag] = []
    @Published var suggestedUsers: [SearchAccount] = []

    // Combine cancellables
    private var cancellables = Set<AnyCancellable>()

    // Use Cases
    private let searchPostsUseCase: SearchPostsUseCase
    private let searchHashtagsUseCase: SearchHashtagsUseCase
    private let searchNewsUseCase: SearchNewsUseCase
    private let getSuggestedAccountsUseCase: GetSuggestedAccountsUseCase

    init() {
        let repo = SearchRepositoryImpl(client: SupabaseClient().client)
        self.searchPostsUseCase = SearchPostsUseCase(repository: repo)
        self.searchHashtagsUseCase = SearchHashtagsUseCase(repository: repo)
        self.searchNewsUseCase = SearchNewsUseCase(repository: repo)
        self.getSuggestedAccountsUseCase = GetSuggestedAccountsUseCase(repository: repo)

        setupBindings()
        loadDiscoveryContent()
    }

    private func setupBindings() {
        // Observe search query with debounce
        $searchQuery
            .dropFirst()
            .debounce(for: .milliseconds(500), scheduler: RunLoop.main)
            .removeDuplicates()
            .sink { [weak self] query in
                self?.performSearch(query: query)
            }
            .store(in: &cancellables)

        // Re-search when category changes
        $selectedCategory
            .dropFirst()
            .sink { [weak self] _ in
                guard let self = self else { return }
                self.performSearch(query: self.searchQuery)
            }
            .store(in: &cancellables)
    }

    func loadDiscoveryContent() {
        Task {
            isLoading = true
            errorMessage = nil
            do {
                let hashtagsResult = try await searchHashtagsUseCase.invoke(query: "")
                if let hashtags = hashtagsResult.getOrNull() as? [SearchHashtag] {
                    self.trendingHashtags = hashtags
                }

                let accountsResult = try await getSuggestedAccountsUseCase.invoke(query: "")
                if let accounts = accountsResult.getOrNull() as? [SearchAccount] {
                    self.suggestedUsers = accounts
                }
            } catch {
                errorMessage = error.localizedDescription
            }
            isLoading = false
        }
    }

    func performSearch(query: String) {
        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            clearResults()
            return
        }

        Task {
            isLoading = true
            errorMessage = nil

            do {
                switch selectedCategory {
                case .users:
                    let result = try await getSuggestedAccountsUseCase.invoke(query: query)
                    if let users = result.getOrNull() as? [SearchAccount] {
                        self.userResults = users
                    } else if let error = result.exceptionOrNull() {
                        self.errorMessage = error.localizedDescription
                    }
                case .posts:
                    let result = try await searchPostsUseCase.invoke(query: query)
                    if let posts = result.getOrNull() as? [SearchPost] {
                        self.postResults = posts
                    } else if let error = result.exceptionOrNull() {
                        self.errorMessage = error.localizedDescription
                    }
                case .hashtags:
                    let result = try await searchHashtagsUseCase.invoke(query: query)
                    if let hashtags = result.getOrNull() as? [SearchHashtag] {
                        self.hashtagResults = hashtags
                    } else if let error = result.exceptionOrNull() {
                        self.errorMessage = error.localizedDescription
                    }
                case .news:
                    let result = try await searchNewsUseCase.invoke(query: query)
                    if let news = result.getOrNull() as? [SearchNews] {
                        self.newsResults = news
                    } else if let error = result.exceptionOrNull() {
                        self.errorMessage = error.localizedDescription
                    }
                }
            } catch {
                self.errorMessage = error.localizedDescription
            }

            isLoading = false
        }
    }

    func clearResults() {
        userResults = []
        postResults = []
        hashtagResults = []
        newsResults = []
    }
}
