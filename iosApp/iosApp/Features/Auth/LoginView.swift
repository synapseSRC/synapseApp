import SwiftUI
import shared

struct LoginView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeAuthViewModel()

    var body: some View {
        ZStack {
            VStack(spacing: AppTheme.Spacing.space20) {
                Text(String(localized: "auth_login_welcome_back"))
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(String(localized: "auth_login_subtitle"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let error = viewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }

                TextField(String(localized: "auth_login_email_placeholder"), text: $viewModel.email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                SecureField(String(localized: "auth_login_password_placeholder"), text: $viewModel.password)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                Spacer()

                Button(action: {
                    viewModel.login(navigator: navigator)
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
                .disabled(viewModel.isLoading)

                if DependencyContainer.shared.passkeyPlatformManager.isSupported {
                    Button(action: {
                        viewModel.signInWithPasskey(navigator: navigator)
                    }) {
                        Label(String(localized: "auth_login_passkey"), systemImage: "person.badge.key")
                            .font(.headline)
                            .foregroundColor(.blue)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(AppTheme.primaryColor.opacity(0.1))
                            .cornerRadius(10)
                    }
                    .padding(.horizontal)
                    .disabled(viewModel.isLoading)
                }

                Spacer()
            }
            .padding()

            if viewModel.isLoading {
                LoadingView(message: String(localized: "auth_login_loading"))
            }
        }
        .navigationTitle(String(localized: "auth_login_button"))
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
