import Foundation
import shared
import Combine

struct NotificationItem: Identifiable, Hashable {
    let id: String
    let type: String
    let actorId: String?
    let actorName: String
    let actorAvatar: String?
    let message: String
    let timestamp: String
    let isRead: Bool
    let targetId: String?
    let target: shared.NotificationTarget

    static func == (lhs: NotificationItem, rhs: NotificationItem) -> Bool {
        return lhs.id == rhs.id && lhs.isRead == rhs.isRead
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(isRead)
    }
}

@MainActor
final class NotificationsViewModel: ObservableObject {
    @Published var notifications: [NotificationItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil

    private let getNotificationsUseCase = KMPHelper.sharedHelper.getNotificationsUseCase
    private let markNotificationAsReadUseCase = KMPHelper.sharedHelper.markNotificationAsReadUseCase
    private let subscribeToNotificationsUseCase = KMPHelper.sharedHelper.subscribeToNotificationsUseCase

    private var loadTask: Task<Void, Never>?

    init() {}

    func loadNotifications() async {
        isLoading = true
        errorMessage = nil

        do {
            for try await result in getNotificationsUseCase.invoke() {
                if let data = result.getOrNull() as? [shared.Notification] {
                    let filtered = data.filter { isEligibleNotification($0) }
                    self.notifications = filtered.map { self.mapDomainToUI($0) }
                } else if let error = result.exceptionOrNull() {
                    self.errorMessage = error.message ?? "Failed to fetch notifications"
                }
                self.isLoading = false
            }
        } catch {
            self.errorMessage = error.localizedDescription
            self.isLoading = false
        }
    }

    private func isEligibleNotification(_ notification: shared.Notification) -> Bool {
        let upper = notification.type.uppercased()
        return upper != "NEW_MESSAGE" && upper != "MESSAGE" && upper != "CHAT"
    }

    func markAsRead(_ id: String) {
        if let index = notifications.firstIndex(where: { $0.id == id }) {
            let item = notifications[index]
            notifications[index] = NotificationItem(
                id: item.id,
                type: item.type,
                actorId: item.actorId,
                actorName: item.actorName,
                actorAvatar: item.actorAvatar,
                message: item.message,
                timestamp: item.timestamp,
                isRead: true,
                targetId: item.targetId,
                target: item.target
            )
        }

        Task {
            do {
                try await markNotificationAsReadUseCase.invoke(notificationId: id)
            } catch {
                if let index = notifications.firstIndex(where: { $0.id == id }) {
                    let item = notifications[index]
                    notifications[index] = NotificationItem(
                        id: item.id,
                        type: item.type,
                        actorId: item.actorId,
                        actorName: item.actorName,
                        actorAvatar: item.actorAvatar,
                        message: item.message,
                        timestamp: item.timestamp,
                        isRead: false,
                        targetId: item.targetId,
                        target: item.target
                    )
                }
                self.errorMessage = error.localizedDescription
            }
        }
    }

    func markAllAsRead() {
        let unreadNotifications = notifications.filter { !$0.isRead }
        for notification in unreadNotifications {
            markAsRead(notification.id)
        }
    }

    private func mapDomainToUI(_ notification: shared.Notification) -> NotificationItem {
        let messageText: String
        if notification.messageType == .custom, let message = notification.message {
            messageText = message
        } else {
            messageText = "You have a new notification"
        }

        let actorNameText = notification.actorName ?? "New Activity"

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        var relativeTime = notification.timestamp
        if let date = formatter.date(from: notification.timestamp) ?? ISO8601DateFormatter().date(from: notification.timestamp) {
            let relativeFormatter = RelativeDateTimeFormatter()
            relativeFormatter.unitsStyle = .short
            relativeTime = relativeFormatter.localizedString(for: date, relativeTo: Date())
        }

        return NotificationItem(
            id: notification.id,
            type: notification.type,
            actorId: notification.actorId,
            actorName: actorNameText,
            actorAvatar: notification.actorAvatar,
            message: messageText,
            timestamp: relativeTime,
            isRead: notification.isRead,
            targetId: notification.targetId,
            target: notification.target
        )
    }
}
