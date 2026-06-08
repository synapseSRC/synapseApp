import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            NavigationView {
                VStack {
                    Image(systemName: "globe")
                        .imageScale(.large)
                        .foregroundColor(.accentColor)
                    Text(String(localized: "home_welcome_title"))
                    Text(String(localized: "home_welcome_subtitle"))
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .navigationTitle(String(localized: "nav_home"))
            }
            .tabItem {
                Label(String(localized: "nav_home"), systemImage: "house")
            }

            ConversationsListView()
                .tabItem {
                    Label("Chat", systemImage: "message")
                }
        }
    }
}

            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.circle")
                }
        }
    }
}
