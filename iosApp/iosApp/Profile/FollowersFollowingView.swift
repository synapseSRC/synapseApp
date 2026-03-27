import SwiftUI

struct FollowersFollowingView: View {
    @State private var selectedTab = 0 // 0 for Followers, 1 for Following

    var body: some View {
        VStack {
            Picker("Tabs", selection: $selectedTab) {
                Text("Followers").tag(0)
                Text("Following").tag(1)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding()

            List {
                if selectedTab == 0 {
                    Text("Follower 1")
                    Text("Follower 2")
                } else {
                    Text("Following 1")
                }
            }
        }
        .navigationTitle(selectedTab == 0 ? "Followers" : "Following")
    }
}
