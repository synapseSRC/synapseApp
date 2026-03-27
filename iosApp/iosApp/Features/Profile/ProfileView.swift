import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        NavigationStack(path: $navigator.profilePath) {
            VStack {
                Image(systemName: "person.circle.fill")
                    .resizable()
                    .frame(width: 100, height: 100)
                    .foregroundColor(.gray)
                    .padding()

                Text("Your Profile")
                    .font(.title)
                    .fontWeight(.bold)

                Spacer()
            }
            .navigationTitle("Profile")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        navigator.navigate(to: .settings, on: .profile)
                    }) {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .settings:
                    SettingsView()
                default:
                    Text("Unknown route")
                }
            }
        }
    }
}

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
            .environmentObject(AppNavigator())
    }
}
