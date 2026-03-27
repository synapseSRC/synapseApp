import SwiftUI

struct AppTabView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        TabView(selection: $navigator.selectedTab) {
            HomeView()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(AppTab.home)

            SearchView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(AppTab.search)

            CreateView()
                .tabItem {
                    Label("Create", systemImage: "plus.circle.fill")
                }
                .tag(AppTab.create)

            NotificationsView()
                .tabItem {
                    Label("Notifications", systemImage: "bell.fill")
                }
                .tag(AppTab.notifications)

            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.fill")
                }
                .tag(AppTab.profile)
        }
    }
}

struct AppTabView_Previews: PreviewProvider {
    static var previews: some View {
        AppTabView()
            .environmentObject(AppNavigator())
    }
}
