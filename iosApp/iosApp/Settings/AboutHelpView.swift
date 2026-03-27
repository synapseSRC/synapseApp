import SwiftUI

struct AboutHelpView: View {
    var body: some View {
        Form {
            Section(header: Text("App Info")) {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0").foregroundColor(.secondary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("App Version 1.0.0")

                Link("Terms of Service", destination: URL(string: "https://example.com/terms")!)
                Link("Privacy Policy", destination: URL(string: "https://example.com/privacy")!)
            }
        }
        .navigationTitle("About & Help")
    }
}
