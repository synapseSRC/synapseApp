import SwiftUI

struct LoginAlert: Identifiable {
    let id = UUID()
    let device: String
    let location: String
    let time: String
}

struct LoginAlertsView: View {
    @State private var loginAlerts: [LoginAlert] = [
        LoginAlert(device: "iPhone 15 Pro", location: "New York, US", time: "2 hours ago"),
        LoginAlert(device: "MacBook Air", location: "London, UK", time: "1 day ago"),
        LoginAlert(device: "Pixel 8", location: "Tokyo, JP", time: "3 days ago")
    ]

    var body: some View {
        List {
            if loginAlerts.isEmpty {
                Text("No recent login alerts.")
                    .foregroundColor(.secondary)
            } else {
                ForEach(loginAlerts) { alert in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(alert.device)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text(alert.location)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Text(alert.time)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("Login Alerts")
    }
}
