import SwiftUI

struct ForgotPasswordView: View {
    @ObservedObject var viewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        VStack(spacing: 20) {
            Text(String(localized: "auth_forgot_password_title"))
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)

            Text(String(localized: "auth_forgot_password_description"))
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

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

            Button(action: {
                viewModel.sendPasswordReset()
            }) {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(String(localized: "auth_forgot_password_button"))
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
        }
        .padding(.top, 40)
        .navigationBarTitle(String(localized: "auth_forgot_password_nav_title"), displayMode: .inline)
        .alert(isPresented: $viewModel.isEmailSent) {
            Alert(
                title: Text(String(localized: "auth_forgot_password_alert_title")),
                message: Text(String(localized: "auth_forgot_password_alert_message")),
                dismissButton: .default(Text(String(localized: "chat_ok"))) {
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
    }
}

struct ForgotPasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ForgotPasswordView(viewModel: AuthViewModel())
    }
}
