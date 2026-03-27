import SwiftUI

struct HomeView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeHomeViewModel()

    var body: some View {
        NavigationStack(path: $navigator.homePath) {
            ZStack {
                if let errorMessage = viewModel.errorMessage {
                    ErrorBoundaryView(title: "Error", message: errorMessage) {
                        viewModel.fetchHomeData()
                    }
                } else if viewModel.isLoading {
                    LoadingView(message: "Loading feed...")
                } else {
                    VStack {
                        Text("Home Feed")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Spacer()

                        Button("Go to Settings") {
                            navigator.navigate(to: .settings, on: .home)
                        }
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)

                        Spacer()
                    }
                }
            }
            .navigationTitle("Synapse")
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .settings:
                    SettingsView()
                case .profile:
                    ProfileView()
                default:
                    Text("Unknown route")
                }
            }
            .onAppear {
                viewModel.fetchHomeData()
            }
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
            .environmentObject(AppNavigator())
    }
}
