import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            NavigationView {
                VStack {
                    Image(systemName: "globe")
                        .imageScale(.large)
                        .foregroundColor(.accentColor)
                    Text("Welcome to Synapse")
                    Text("Powered by Kotlin Multiplatform")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .navigationTitle("Home")
            }
            .tabItem {
                Label("Home", systemImage: "house")
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
