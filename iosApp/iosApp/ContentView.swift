//
//  ContentView.swift
//  iosApp
//

import SwiftUI
import SharedLogic

enum AppTab: Hashable {
    case home
    case statistics
    case profile
}

struct ContentView: View {
    @Environment(\.appTheme) private var colors
    @AppStorage("app_theme_mode") private var selectedThemeRaw: String = AppThemeMode.system.rawValue

    @State private var isLoggedIn: Bool = false
    @State private var selectedTab: AppTab = .home

    private var selectedTheme: AppThemeMode {
        AppThemeMode(rawValue: selectedThemeRaw) ?? .system
    }

    var body: some View {
        Group {
            if isLoggedIn {
                MainTabView(
                    selectedTab: $selectedTab,
                    onSessionExpired: {
                        withAnimation(.easeInOut(duration: 0.25)) {
                            isLoggedIn = false
                            selectedTab = .home
                        }
                    }
                )
            } else {
                AuthView(
                    onLoginSuccess: {
                        withAnimation(.easeInOut(duration: 0.25)) {
                            isLoggedIn = true
                        }
                    }
                )
            }
        }
        .tint(colors.indicator)
        .preferredColorScheme(selectedTheme.colorScheme)
    }
}

struct MainTabView: View {
    @Binding var selectedTab: AppTab
    let onSessionExpired: () -> Void
    @Environment(\.appTheme) private var colors

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("home", systemImage: "figure.run", value: AppTab.home) {
                HomeView(onSessionExpired: onSessionExpired)
            }

            Tab("statistics", systemImage: "chart.bar.xaxis", value: AppTab.statistics) {
                StatisticsPlaceholderView()
            }

            Tab("profile", systemImage: "person.crop.circle", value: AppTab.profile) {
                ProfileTabScreen(onLogoutSuccess: onSessionExpired)
            }
        }
        .tint(colors.indicator)
    }
}

private struct StatisticsPlaceholderView: View {
    @Environment(\.appTheme) private var colors

    var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()
            Text("statistics")
                .font(Font.AppTypography.headlineLarge)
                .foregroundStyle(colors.textPrimary)
        }
    }
}
