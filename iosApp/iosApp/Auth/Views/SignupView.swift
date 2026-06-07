import SwiftUI

struct SignupView: View {
    @ObservedObject var viewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        VStack(spacing: 20) {
            Text("auth_signup_title")
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

            TextField("auth_signup_username_placeholder", text: $viewModel.username)
                .autocapitalization(.none)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            SecureField("auth_login_password_placeholder", text: $viewModel.password)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            SecureField("auth_signup_confirm_password_placeholder", text: $viewModel.confirmPassword)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            Button(action: {
                viewModel.signup()
            }) {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("auth_signup_button")
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

            Spacer()

            HStack {
                Text("auth_signup_already_account")
                    .font(.caption)
                Button(action: {
                    presentationMode.wrappedValue.dismiss()
                }) {
                    Text("auth_signup_login_link")
                        .font(.caption)
                        .fontWeight(.bold)
                }
            }
            .padding(.bottom)
        }
        .navigationBarTitle("auth_signup_nav_title", displayMode: .inline)
        .alert(isPresented: $viewModel.isEmailSent) {
            Alert(
                title: Text("auth_signup_alert_title"),
                message: Text("auth_signup_alert_message"),
                dismissButton: .default(Text("chat_ok")) {
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
    }
}

struct SignupView_Previews: PreviewProvider {
    static var previews: some View {
        SignupView(viewModel: AuthViewModel())
    }
}
