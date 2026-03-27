import SwiftUI

struct LoginView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeAuthViewModel()

    var body: some View {
        ZStack {
            VStack(spacing: 20) {
                Text("Welcome Back")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Enter your credentials to continue")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Spacer()

                Button(action: {
                    viewModel.login(navigator: navigator)
                }) {
                    Text("Simulate Login")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                .disabled(viewModel.isLoading)

                Spacer()
            }
            .padding()

            if viewModel.isLoading {
                LoadingView(message: "Logging in...")
            }
        }
        .navigationTitle("Log In")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            LoginView()
                .environmentObject(AppNavigator())
        }
    }
}
