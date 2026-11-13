import SwiftUI

struct DataStorageSettingsView: View {
    @State private var cacheSize = "124 MB"
    @State private var showClearAlert = false
    @State private var showDownloadAlert = false

    var body: some View {
        Form {
            Section(header: Text("Storage Usage")) {
                HStack {
                    Text("App Cache")
                    Spacer()
                    Text(cacheSize).foregroundColor(.secondary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("App Cache Size is \(cacheSize)")

                Button(action: { showClearAlert = true }) {
                    Text("Clear App Cache")
                        .foregroundColor(.red)
                }
                .accessibilityLabel("Clear App Cache Button")
            }

            Section(header: Text("Data Management")) {
                Button(action: { showDownloadAlert = true }) {
                    Text("Request Account Data Download")
                }
                .accessibilityLabel("Request Account Data Download Button")
            }
        }
        .navigationTitle("Data & Storage")
        .alert(isPresented: $showClearAlert) {
            Alert(
                title: Text("Clear Cache?"),
                message: Text("This will free up space but may cause initial loading to be slower."),
                primaryButton: .destructive(Text("Clear")) {
                    cacheSize = "0 MB"
                },
                secondaryButton: .cancel()
            )
        }
        .alert(
            "Account Data Download",
            isPresented: $showDownloadAlert,
            actions: {
                Button("OK", role: .cancel) {}
            },
            message: {
                Text("Account data download isn't available yet. Please check back later.")
            }
        )
    }
}
