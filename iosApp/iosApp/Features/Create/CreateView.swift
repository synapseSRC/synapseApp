import SwiftUI

struct CreateView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        NavigationStack(path: $navigator.createPath) {
            VStack {
                Text("Create New Post")
                    .font(.title)
                    .fontWeight(.bold)
                Spacer()
            }
            .navigationTitle("Create")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct CreateView_Previews: PreviewProvider {
    static var previews: some View {
        CreateView()
            .environmentObject(AppNavigator())
    }
}
