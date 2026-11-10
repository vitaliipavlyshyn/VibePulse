import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    
    init() {
        KoinKt.doInitKoin {koinApp in}
    }
    
    var body: some Scene {
        WindowGroup {
            VibePulseTheme {
                ContentView()
            }
        }
    }
}
