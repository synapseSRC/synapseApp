import SwiftUI
import shared

struct ContentView: View {
	var body: some View {
        NavigationView {
            VStack {
                Image(systemName: "globe")
                    .imageScale(.large)
                    .foregroundColor(.accentColor)
                Text("Welcome to Synapse")
                Text("Powered by Kotlin Multiplatform")
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            .navigationTitle("Home")
        }
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
