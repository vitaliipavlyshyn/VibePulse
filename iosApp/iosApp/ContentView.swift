import SwiftUI
import SharedLogic

struct ContentView: View {
    @State private var isAuthenticated = false

        var body: some View {
            if isAuthenticated {
                Text("Connected to Spotify!")
                    .font(.title2)
            } else {
                AuthView(onLoginSuccess: {
                    isAuthenticated = true
                })
            }
        }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
