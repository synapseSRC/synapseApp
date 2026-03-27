import SwiftUI

struct AuthView: View {
    @EnvironmentObject var navigator: AppNavigator

    var body: some View {
        NavigationStack(path: $navigator.authPath) {
            VStack {
                Spacer()

                Image(systemName: "bolt.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)
                    .padding(.bottom, 20)

                Text("Synapse")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Connect with the world")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.bottom, 40)

                Button(action: {
                    navigator.navigate(to: .login)
                }) {
                    Text("Log In")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                .padding(.bottom, 10)

                Button(action: {
                    navigator.navigate(to: .register)
                }) {
                    Text("Create Account")
                        .font(.headline)
                        .foregroundColor(.blue)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(10)
                }
                .padding(.horizontal)

                Spacer()
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .login:
                    LoginView()
                case .register:
                    RegisterView()
                default:
                    EmptyView()
                }
            }
        }
    }
}

struct AuthView_Previews: PreviewProvider {
    static var previews: some View {
        AuthView()
            .environmentObject(AppNavigator())
    }
}
