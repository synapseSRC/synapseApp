import SwiftUI

struct SignupView: View {
    @ObservedObject var viewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        VStack(spacing: 20) {
            Text("Create Account")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 20)

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

            TextField("Username", text: $viewModel.username)
                .autocapitalization(.none)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            SecureField("Password", text: $viewModel.password)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            SecureField("Confirm Password", text: $viewModel.confirmPassword)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)

            Button(action: {
                viewModel.signup()
            }) {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text("Sign Up")
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
                Text("Already have an account?")
                    .font(.caption)
                Button(action: {
                    presentationMode.wrappedValue.dismiss()
                }) {
                    Text("Log In")
                        .font(.caption)
                        .fontWeight(.bold)
                }
            }
            .padding(.bottom)
        }
        .navigationBarTitle("Sign Up", displayMode: .inline)
        .alert(isPresented: $viewModel.isEmailSent) {
            Alert(
                title: Text("Verify Email"),
                message: Text("We've sent a verification link to your email."),
                dismissButton: .default(Text("OK")) {
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
