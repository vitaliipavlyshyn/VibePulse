import SwiftUI
import SharedLogic

struct ContentView: View {
    @State private var showContent = false
    
    var body: some View {
          
            VibePulseTheme {
                AppTheme.colors.background
                    .ignoresSafeArea()
                    .overlay(
                        VStack(spacing: 24) {
                            Button("Click me!") {
                                withAnimation {
                                    showContent = !showContent
                                }
                            }
                            .font(Font.AppTypography.labelLarge)
                            .foregroundColor(AppTheme.colors.primary)

                            if showContent {
                                VStack(spacing: 16) {
                                    Image(systemName: "swift")
                                        .font(.system(size: 100))
                                        .foregroundColor(AppTheme.colors.primary)
                                        
                                    Text("SwiftUI: \(Greeting().greet())")
                                        .font(Font.AppTypography.bodyLarge)
                                        .foregroundColor(AppTheme.colors.onBackground)
                                }
                                .padding()
                                .background(AppTheme.colors.surface)
                                .cornerRadius(12)
                                .transition(.move(edge: .top).combined(with: .opacity))
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .padding()
                    )
            }
        }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
