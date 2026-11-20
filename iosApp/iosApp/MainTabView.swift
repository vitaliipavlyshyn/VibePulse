import SwiftUI

struct MainTabView: View {
    @Binding var selectedTab: AppTab
    let onSessionExpired: () -> Void

    var body: some View {
        VibePulseTheme {
            TabView(selection: $selectedTab) {
                HomeView(
                    onSessionExpired: onSessionExpired,
                    onNavigateToProfile: { selectedTab = .profile }
                )
                .tabItem {
                    Label("home", systemImage: "figure.run")
                }
                .tag(AppTab.home)

                StatisticsView()
                    .tabItem {
                        Label("statistics", systemImage: "chart.bar.fill")
                    }
                    .tag(AppTab.statistics)

                ProfileView(onLogoutSuccess: onSessionExpired)
                    .tabItem {
                        Label("profile", systemImage: "person.fill")
                    }
                    .tag(AppTab.profile)
            }
        }
    }
}
