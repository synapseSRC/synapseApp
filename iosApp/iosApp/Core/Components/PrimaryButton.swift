import SwiftUI

struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var backgroundColor: Color = .blue
    var foregroundColor: Color = .white

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .foregroundColor(foregroundColor)
                .frame(maxWidth: .infinity)
                .padding()
                .background(backgroundColor)
                .cornerRadius(10)
        }
        .padding(.horizontal)
    }
}

struct PrimaryButton_Previews: PreviewProvider {
    static var previews: some View {
        PrimaryButton(title: "Test Button", action: {})
    }
}
