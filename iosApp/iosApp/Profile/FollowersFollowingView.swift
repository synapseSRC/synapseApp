import SwiftUI

struct FollowersFollowingView: View {
    @State private var selectedTab = 0 // 0 for Followers, 1 for Following

    var body: some View {
        VStack {
            Picker("Tabs", selection: $selectedTab) {
                Text(String(localized: "profile_tab_followers")).tag(0)
                Text(String(localized: "profile_tab_following")).tag(1)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding()

            List {
                // TODO: Replace stub data with actual followers/following list
                Group {
                    if selectedTab == 0 {
                        // Empty state for Followers
                    } else {
                        // Empty state for Following
                    }
                }
            }
        }
        .navigationTitle(selectedTab == 0 ? String(localized: "profile_tab_followers") : String(localized: "profile_tab_following"))
    }
}
