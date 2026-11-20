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

    @State private var isLoggedIn: Bool = KoinHelper().hasSession()
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
