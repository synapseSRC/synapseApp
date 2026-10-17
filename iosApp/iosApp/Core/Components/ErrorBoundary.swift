import SwiftUI

struct ErrorBoundaryView: View {
    let title: String
    let message: String
    let retryAction: () -> Void

    var body: some View {
        VStack(spacing: AppTheme.Spacing.space16) {
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
                .padding(.horizontal, AppTheme.Spacing.space32)

            Button(action: retryAction) {
                Text(String(localized: "action_try_again"))
                    .fontWeight(.bold)
                    .padding(.horizontal, AppTheme.Spacing.space24)
                    .padding(.vertical, AppTheme.Spacing.space12)
                    .background(AppTheme.primaryColor)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .padding(.top, AppTheme.Spacing.space8)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorBoundaryView_Previews: PreviewProvider {
    static var previews: some View {
        ErrorBoundaryView(
            title: String(localized: "error_connection_title"),
            message: String(localized: "error_connection_message"),
            retryAction: {}
        )
    }
}
