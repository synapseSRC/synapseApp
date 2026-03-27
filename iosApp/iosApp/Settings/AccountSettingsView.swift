import SwiftUI
struct AccountSettingsView: View {
    @State private var email = ""
    var body: some View {
        Form {
            Section(header: Text("Contact Info")) {
                TextField("Email Address", text: $email)
            }
        }
        .navigationTitle("Account")
    }
}
