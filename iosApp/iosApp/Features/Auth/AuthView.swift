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

                Text(String(localized: "app_name"))
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(String(localized: "auth_tagline"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.bottom, 40)

                Button(action: {
                    navigator.navigate(to: .login)
                }) {
                    Text(String(localized: "auth_login_button"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppTheme.primaryColor)
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                .padding(.bottom, 10)

                Button(action: {
                    navigator.navigate(to: .register)
                }) {
                    Text(String(localized: "action_create_account"))
                        .font(.headline)
                        .foregroundColor(.blue)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppTheme.primaryColor.opacity(0.1))
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
