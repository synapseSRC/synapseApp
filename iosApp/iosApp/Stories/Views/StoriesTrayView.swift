import SwiftUI

struct StoriesTrayView: View {
    @StateObject private var viewModel = StoriesViewModel()
    @State private var showingCreator = false
    @State private var showingViewer = false
    @State private var selectedStoryIndex = 0
    @State private var showingDeleteActionSheet = false

    private func buildAllGroups() -> [StoryGroup] {
        var groups: [StoryGroup] = []
        if let myStory = viewModel.myStory {
            groups.append(myStory)
        }
        groups.append(contentsOf: viewModel.stories)
        return groups
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                // "Your Story" Item
                StoryAvatarView(
                    imageUrl: viewModel.myStory?.user.avatar.flatMap { URL(string: $0) },
                    title: "Your Story",
                    isAddButton: viewModel.myStory == nil,
                    hasUnseen: viewModel.myStory?.hasUnseen ?? false,
                    action: {
                        if viewModel.myStory == nil {
                            showingCreator = true
                        } else {
                            // View own story
                            selectedStoryIndex = -1 // flag for own story
                            showingViewer = true
                        }
                    }
                )
                .onLongPressGesture {
                    if viewModel.myStory != nil {
                        showingDeleteActionSheet = true
                    }
                }
                .padding(.leading, 12)

                // Other Users' Stories
                ForEach(Array(viewModel.stories.enumerated()), id: \.element.id) { index, group in
                    StoryAvatarView(
                        imageUrl: group.user.avatar.flatMap { URL(string: $0) },
                        title: group.user.displayName ?? group.user.username ?? "User",
                        isAddButton: false,
                        hasUnseen: group.hasUnseen,
                        action: {
                            selectedStoryIndex = index
                            showingViewer = true
                        }
                    )
                }
            }
            .padding(.vertical, 8)
        }
        .overlay(
            Group {
                if let error = viewModel.error {
                    HStack {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                            .lineLimit(1)
                        Button(action: {
                            viewModel.retryLoadStories()
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.caption)
                                .foregroundColor(.blue)
                        }
                    }
                    .padding(8)
                    .background(Color.black.opacity(0.7).cornerRadius(8))
                } else if viewModel.isLoading && viewModel.stories.isEmpty && viewModel.myStory == nil {
                    ProgressView()
                }
            }
        )
        .fullScreenCover(isPresented: $showingCreator) {
            StoryCreatorScreen()
        }
        .fullScreenCover(isPresented: $showingViewer) {
            let allGroups = buildAllGroups()
            let initialIndex = selectedStoryIndex == -1 ? 0 : (viewModel.myStory != nil ? selectedStoryIndex + 1 : selectedStoryIndex)

            if !allGroups.isEmpty {
                StoryViewerScreen(
                    storyGroups: allGroups,
                    initialIndex: initialIndex,
                    isOwnStory: viewModel.myStory != nil
                )
            } else {
                Text("Error loading story")
            }
        }
        .actionSheet(isPresented: $showingDeleteActionSheet) {
            ActionSheet(
                title: Text("Story Options"),
                message: Text("What would you like to do?"),
                buttons: [
                    .destructive(Text("Delete Story")) {
                        if let storyId = viewModel.myStory?.stories.first?.id {
                            viewModel.deleteStory(storyId)
                        }
                    },
                    .cancel()
                ]
            )
        }
    }
}
