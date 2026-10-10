import SwiftUI

enum AppRoute: Hashable {
    case home
    case search
    case create
    case notifications
    case profile
    case settings
    case login
    case register
}

enum AppTab: Int {
    case home = 0
    case search = 1
    case create = 2
    case notifications = 3
    case profile = 4
}

class AppNavigator: ObservableObject {
    @Published var homePath = NavigationPath()
    @Published var searchPath = NavigationPath()
    @Published var createPath = NavigationPath()
    @Published var notificationsPath = NavigationPath()
    @Published var profilePath = NavigationPath()
    @Published var authPath = NavigationPath()

    @Published var isUserLoggedIn: Bool = false
    @Published var selectedTab: AppTab = .home

    func navigate(to route: AppRoute, on tab: AppTab? = nil) {
        let activeTab = tab ?? selectedTab

        if !isUserLoggedIn {
            authPath.append(route)
            return
        }

        switch activeTab {
        case .home:
            homePath.append(route)
        case .search:
            searchPath.append(route)
        case .create:
            createPath.append(route)
        case .notifications:
            notificationsPath.append(route)
        case .profile:
            profilePath.append(route)
        }
    }

    func goBack(on tab: AppTab? = nil) {
        let activeTab = tab ?? selectedTab

        if !isUserLoggedIn {
            if !authPath.isEmpty { authPath.removeLast() }
            return
        }

        switch activeTab {
        case .home:
            if !homePath.isEmpty { homePath.removeLast() }
        case .search:
            if !searchPath.isEmpty { searchPath.removeLast() }
        case .create:
            if !createPath.isEmpty { createPath.removeLast() }
        case .notifications:
            if !notificationsPath.isEmpty { notificationsPath.removeLast() }
        case .profile:
            if !profilePath.isEmpty { profilePath.removeLast() }
        }
    }

    func reset() {
        homePath = NavigationPath()
        searchPath = NavigationPath()
        createPath = NavigationPath()
        notificationsPath = NavigationPath()
        profilePath = NavigationPath()
        authPath = NavigationPath()
        selectedTab = .home
    }
}
