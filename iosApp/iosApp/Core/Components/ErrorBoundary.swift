import SwiftUI

struct ErrorBoundaryView: View {
    let title: String
    let message: String
    let retryAction: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 60, height: 60)
                .foregroundColor(.red)

            Text(title)
                .font(.title2)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)

            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button(action: retryAction) {
                Text("Try Again")
                    .fontWeight(.bold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .padding(.top, 8)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorBoundaryView_Previews: PreviewProvider {
    static var previews: some View {
        ErrorBoundaryView(
            title: "Connection Error",
            message: "Failed to connect to the server. Please check your internet connection and try again.",
            retryAction: {}
        )
    }
}
