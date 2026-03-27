import SwiftUI

struct SearchView: View {
    @EnvironmentObject var navigator: AppNavigator
    @State private var searchText = ""

    var body: some View {
        NavigationStack(path: $navigator.searchPath) {
            VStack {
                Text("Search & Explore")
                    .font(.title)
                    .fontWeight(.bold)
                Spacer()
            }
            .navigationTitle("Search")
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
