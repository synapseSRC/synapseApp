import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var navigator: AppNavigator
    @StateObject private var viewModel = DependencyContainer.shared.makeAuthViewModel()

    var body: some View {
        ZStack {
            VStack(spacing: 20) {
                Text(String(localized: "action_create_account"))
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(String(localized: "register_subtitle"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let error = viewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .multilineTextAlignment(.center)
                }

                TextField(String(localized: "field_email"), text: $viewModel.email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                TextField(String(localized: "field_username"), text: $viewModel.username)
                    .autocapitalization(.none)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                SecureField(String(localized: "field_password"), text: $viewModel.password)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                SecureField(String(localized: "field_confirm_password"), text: $viewModel.confirmPassword)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                Spacer()

                Button(action: {
                    viewModel.register(navigator: navigator)
                }) {
                    Text(String(localized: "action_register"))
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
