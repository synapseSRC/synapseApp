import SwiftUI
import LocalAuthentication

struct LoginView: View {
    @ObservedObject var viewModel: AuthViewModel

    // Navigation state
    @State private var showSignup = false
    @State private var showForgotPassword = false

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("auth_login_welcome_back")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.bottom, 20)

                if let error = viewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }

                TextField("auth_login_email_placeholder", text: $viewModel.email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                SecureField("auth_login_password_placeholder", text: $viewModel.password)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                HStack {
                    Spacer()
                    Button(action: {
                        showForgotPassword = true
                    }) {
                        Text("auth_login_forgot_password")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                    .padding(.trailing)
                }

                Button(action: {
                    viewModel.login()
                }) {
                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("auth_login_button")
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding()
                .background(Color.blue)
                .cornerRadius(10)
                .padding(.horizontal)
                .disabled(viewModel.isLoading)

                // Biometrics Button
                Button(action: {
                    viewModel.authenticateWithBiometrics()
                }) {
                    HStack {
                        Image(systemName: "faceid")
                        Text("auth_login_face_id")
                    }
                    .padding()
                    .foregroundColor(.blue)
                }

                Spacer()

                Text("auth_login_social_divider")
                    .font(.caption)
                    .foregroundColor(.gray)

                HStack(spacing: 20) {
                    Image(systemName: "applelogo")
                        .font(.title)
                    Image(systemName: "g.circle")
                        .font(.title)
                }
                .padding(.bottom, 30)

                HStack {
                    Text("auth_login_no_account")
                        .font(.caption)
                    Button(action: {
                        showSignup = true
                    }) {
                        Text("auth_login_signup_link")
                            .font(.caption)
                            .fontWeight(.bold)
                    }
                }
                .padding(.bottom)

                // Navigation links
                NavigationLink(destination: SignupView(viewModel: viewModel), isActive: $showSignup) {
                    EmptyView()
                }
                NavigationLink(destination: ForgotPasswordView(viewModel: viewModel), isActive: $showForgotPassword) {
                    EmptyView()
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView(viewModel: AuthViewModel())
    }
}
