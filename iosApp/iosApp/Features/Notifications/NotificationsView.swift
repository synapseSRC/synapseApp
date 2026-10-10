import SwiftUI
import shared

struct NotificationsView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeNotificationsViewModel()
    @State private var hasInitialFetch = false

    var body: some View {
        NavigationStack(path: $navigator.notificationsPath) {
            ZStack {
                if viewModel.isLoading && viewModel.notifications.isEmpty {
                    ProgressView("Loading notifications...")
                } else if let errorMessage = viewModel.errorMessage, viewModel.notifications.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.largeTitle)
                            .foregroundColor(.red)
                        Text(String(localized: "label_error"))
                            .font(.headline)
                        Text(errorMessage)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                        Button("Retry") {
                            Task {
                                await viewModel.loadNotifications()
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                } else if viewModel.notifications.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "bell.slash")
                            .font(.largeTitle)
                            .foregroundColor(.secondary)
                        Text(String(localized: "notifications_empty_title"))
                            .font(.headline)
                        Text(String(localized: "notifications_empty_subtitle"))
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                            let notificationGroups = Dictionary(grouping: viewModel.notifications, by: { $0.timestamp })
                            let sortedKeys = notificationGroups.keys.sorted(by: >)

                            ForEach(sortedKeys, id: \.self) { dateGroup in
                                Section(header: sectionHeaderView(title: dateGroup)) {
                                    if let groupItems = notificationGroups[dateGroup] {
                                        VStack(spacing: 0) {
                                            ForEach(Array(groupItems.enumerated()), id: \.element.id) { index, notification in
                                                let pos: GroupPosition = {
                                                    if groupItems.count == 1 { return .single }
                                                    if index == 0 { return .first }
                                                    if index == groupItems.count - 1 { return .last }
                                                    return .middle
                                                }()

                                                VStack(spacing: 0) {
                                                    NotificationRowView(
                                                        notification: notification,
                                                        position: pos,
                                                        onActorTap: {
                                                            if let actorId = notification.actorId {
                                                                navigator.navigate(to: .profile)
                                                            }
                                                        }
                                                    )
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        if !notification.isRead {
                                                            viewModel.markAsRead(notification.id)
                                                        }
                                                        handleTargetNavigation(notification: notification)
                                                    }

                                                    if index < groupItems.count - 1 {
                                                        Divider()
                                                            .padding(.horizontal, 16)
                                                            .background(Color(uiColor: .tertiarySystemGroupedBackground))
                                                    }
                                                }
                                            }
                                        }
                                        .padding(.horizontal, 16)
                                        .padding(.bottom, 12)
                                    }
                                }
                            }
                        }
                    }
                    .refreshable {
                        await viewModel.loadNotifications()
                    }
                }
            }
            .navigationTitle(String(localized: "nav_notifications"))
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !viewModel.notifications.isEmpty {
                        Button("Mark all as read") {
                            viewModel.markAllAsRead()
                        }
                        .font(.subheadline)
                    }
                }
            }
            .onAppear {
                if !hasInitialFetch {
                    hasInitialFetch = true
                    Task {
                        await viewModel.loadNotifications()
                    }
                }
            }
        }
    }

    private func sectionHeaderView(title: String) -> some View {
        HStack {
            Text(title)
                .font(.footnote)
                .fontWeight(.bold)
                .foregroundColor(.accentColor)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(uiColor: .systemBackground))
    }

    private func handleTargetNavigation(notification: NotificationItem) {
        if let targetPost = notification.target as? shared.NotificationTargetPost {
            navigator.navigate(to: .home)
        } else if let targetComment = notification.target as? shared.NotificationTargetComment {
            navigator.navigate(to: .home)
        } else if let targetProfile = notification.target as? shared.NotificationTargetProfile {
            navigator.navigate(to: .profile)
        } else if let targetId = notification.targetId, !targetId.isEmpty {
            navigator.navigate(to: .home)
        }
    }
}

struct NotificationsView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationsView()
            .environmentObject(AppNavigator())
    }
}
