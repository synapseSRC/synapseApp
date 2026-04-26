import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeAuthViewModel()

    var body: some View {
        ZStack {
            VStack(spacing: 20) {
                Text("Create Account")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Join Synapse and connect with others")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

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

                Spacer()

                Button(action: {
                    viewModel.register(navigator: navigator)
                }) {
                    Text("Register")
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
                LoadingView(message: "Creating account...")
            }
        }
        .navigationTitle("Register")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct RegisterView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            RegisterView()
                .environmentObject(AppNavigator())
        }
    }
}
