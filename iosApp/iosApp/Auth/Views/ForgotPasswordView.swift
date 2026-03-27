import SwiftUI

struct ForgotPasswordView: View {
    @ObservedObject var viewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        VStack(spacing: 20) {
            Text("Reset Password")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)

            Text("Enter your email address to receive a password reset link.")
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

            TextField("Email", text: $viewModel.email)
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
                    Text("Send Reset Link")
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
        .navigationBarTitle("Forgot Password", displayMode: .inline)
        .alert(isPresented: $viewModel.isEmailSent) {
            Alert(
                title: Text("Link Sent"),
                message: Text("Check your email for the password reset link."),
                dismissButton: .default(Text("OK")) {
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
