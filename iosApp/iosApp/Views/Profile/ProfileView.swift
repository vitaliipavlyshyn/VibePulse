//
//  ProfileView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI

enum AppThemeMode: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }

    var localizedTitle: LocalizedStringKey {
        switch self {
        case .system: return "profile_theme_system"
        case .light: return "profile_theme_light"
        case .dark: return "profile_theme_dark"
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

struct ProfileView: View {
    @Environment(\.appTheme) private var colors
    @AppStorage("app_theme_mode") private var selectedThemeRaw: String = AppThemeMode.system.rawValue
    @State private var showLogoutConfirmation = false

    let displayName: String?
    let avatarUrl: String?
    let isPremium: Bool
    let availableGenres: [String]
    let selectedGenres: Set<String>
    let weatherEnabled: Bool
    let excludeRemixesAndCovers: Bool
    let excludeExplicit: Bool

    let onToggleGenre: (String) -> Void
    let onWeatherToggle: (Bool) -> Void
    let onExcludeRemixesToggle: (Bool) -> Void
    let onExcludeExplicitToggle: (Bool) -> Void
    let onLogoutConfirm: () -> Void

    private var selectedTheme: AppThemeMode {
        AppThemeMode(rawValue: selectedThemeRaw) ?? .system
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                ProfileHeaderSection(
                    displayName: displayName,
                    avatarUrl: avatarUrl,
                    isPremium: isPremium
                )

                MusicPreferencesCard(
                    isPremium: isPremium,
                    availableGenres: availableGenres,
                    selectedGenres: selectedGenres,
                    weatherEnabled: weatherEnabled,
                    excludeRemixesAndCovers: excludeRemixesAndCovers,
                    excludeExplicit: excludeExplicit,
                    onToggleGenre: onToggleGenre,
                    onWeatherToggle: onWeatherToggle,
                    onExcludeRemixesToggle: onExcludeRemixesToggle,
                    onExcludeExplicitToggle: onExcludeExplicitToggle
                )

                ThemeSelectionCard(
                    selectedMode: selectedTheme,
                    onSelectTheme: { mode in
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.72)) {
                            selectedThemeRaw = mode.rawValue
                        }
                    }
                )

                LogoutButton {
                    showLogoutConfirmation = true
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
        .background(colors.background.ignoresSafeArea())
        .alert("profile_logout_dialog_title", isPresented: $showLogoutConfirmation) {
            Button("profile_logout_dialog_confirm", role: .destructive) {
                onLogoutConfirm()
            }
            Button("profile_logout_dialog_cancel", role: .cancel) {}
        } message: {
            Text("profile_logout_dialog_message")
        }
    }
}
