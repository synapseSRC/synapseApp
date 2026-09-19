import SwiftUI

struct SearchView: View {
    @EnvironmentObject var navigator: AppNavigator
    @State private var searchText = ""

    var body: some View {
        NavigationStack(path: $navigator.searchPath) {
            VStack {
                Text(String(localized: "search_title"))
                    .font(.title)
                    .fontWeight(.bold)
                Spacer()
            }
            .navigationTitle(String(localized: "nav_search"))
        }
        .searchable(text: $searchText, prompt: "Find people, tags, and posts")
    }
}

struct SearchView_Previews: PreviewProvider {
    static var previews: some View {
        SearchView()
            .environmentObject(AppNavigator())
    }
}
