import Foundation
import shared

struct StoryGroup: Identifiable {
    let id: String // userId
    let user: shared.User
    let stories: [shared.Story]
    var hasUnseen: Bool
}

@MainActor
class StoriesViewModel: ObservableObject {
    @Published var stories: [StoryGroup] = []
    @Published var isLoading = false
    @Published var error: String? = nil
    @Published var myStory: StoryGroup? = nil

    private let getStoriesUseCase = KMPHelper.sharedHelper.getStoriesUseCase
    // Placeholder for when you need auth info
    // private let currentUserId = ...

    init() {
        loadStories()
    }

    func retryLoadStories() {
        loadStories()
    }

    func loadStories() {
        self.isLoading = true
        self.error = nil

        let currentUserId = DependencyContainer.shared.authRepository.getCurrentUserId()

        Task {
            do {
                let domainStories = try await getStoriesUseCase.invoke()

                // Group stories by userId
                var groupedDict: [String: [shared.Story]] = [:]
                for story in domainStories {
                    groupedDict[story.userId, default: []].append(story)
                }

                var newGroups: [StoryGroup] = []
                for (userId, userStories) in groupedDict {
                    // Fetch real user object via KMP use case
                    var resolvedUser: shared.User? = nil
                    do {
                        let userResult = try await KMPHelper.sharedHelper.getUserProfileUseCase.invoke(uid: userId)
                        // In Swift, Result<T> maps to the success type if successful, or throws
                        if let userObj = userResult as? shared.User {
                            resolvedUser = userObj
                        }
                    } catch {
                        print("Failed to fetch user \(userId): \(error)")
                    }

                    // Fallback dummy user if fetch fails
                    let user = resolvedUser ?? shared.User(
                        id: userId,
                        uid: userId,
                        email: nil,
                        username: "User",
                        nickname: nil,
                        displayName: "User",
                        name: nil,
                        bio: nil,
                        avatar: nil,
                        avatarHistoryType: "local",
                        profileCoverImage: nil,
                        coverImageUrl: nil,
                        accountPremium: false,
                        userLevelXp: 0,
                        verify: false,
                        isVerified: false,
                        isPrivate: false,
                        accountType: "user",
                        gender: "hidden",
                        banned: false,
                        status: .offline,
                        joinDate: nil,
                        joinedDate: 0,
                        oneSignalPlayerId: nil,
                        lastSeen: nil,
                        chattingWith: nil,
                        createdAt: nil,
                        updatedAt: nil,
                        followersCount: 0,
                        followingCount: 0,
                        postsCount: 0,
                        postCount: 0,
                        followerCount: 0,
                        location: nil,
                        relationshipStatus: nil,
                        birthday: nil,
                        work: nil,
                        education: nil,
                        currentCity: nil,
                        hometown: nil,
                        website: nil,
                        pronouns: nil,
                        linkedAccounts: [],
                        privacySettings: [:]
                    )

                    let group = StoryGroup(
                        id: userId,
                        user: user,
                        stories: userStories.sorted { ($0.createdAt ?? "") < ($1.createdAt ?? "") },
                        hasUnseen: true // Default to true for now, can implement specific viewed tracking logic later
                    )

                    if userId == currentUserId {
                        self.myStory = group
                    } else {
                        newGroups.append(group)
                    }
                }

                // Own story is stored separately in `myStory`.
                // Sort the rest by unseen first, then by latest.
                self.stories = newGroups.sorted {
                    if $0.hasUnseen != $1.hasUnseen {
                        return $0.hasUnseen && !$1.hasUnseen
                    }
                    return $0.id < $1.id // fallback sort
                }
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func markStoryAsSeen(_ storyId: String) {
        let viewerId = DependencyContainer.shared.authRepository.getCurrentUserId()
        guard let viewerId = viewerId else { return }

        Task {
            do {
                try await KMPHelper.sharedHelper.markStoryAsSeenUseCase.invoke(storyId: storyId, viewerId: viewerId)
            } catch {
                print("Failed to mark story as seen: \(error.localizedDescription)")
            }
        }
    }

    func deleteStory(_ storyId: String) {
        Task {
            do {
                try await KMPHelper.sharedHelper.deleteStoryUseCase.invoke(storyId: storyId)
                loadStories()
            } catch {
                print("Failed to delete story: \(error.localizedDescription)")
            }
        }
    }
}
