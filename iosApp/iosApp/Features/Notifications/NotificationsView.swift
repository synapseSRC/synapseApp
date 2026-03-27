import SwiftUI

struct NotificationsView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        NavigationStack(path: $navigator.notificationsPath) {
            VStack {
                Text("Your Notifications")
                    .font(.title)
                    .fontWeight(.bold)
                Spacer()
            }
            .navigationTitle("Notifications")
        }
    }
}

struct NotificationsView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationsView()
            .environmentObject(AppNavigator())
    }
}
