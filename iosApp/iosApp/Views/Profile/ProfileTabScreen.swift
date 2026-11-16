//
//  ProfileTabScreen.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct ProfileTabScreen: View {
    let onLogoutSuccess: () -> Void

    @State private var viewModel: ProfileViewModel
    @State private var uiState: ProfileUiState

    init(onLogoutSuccess: @escaping () -> Void) {
        self.onLogoutSuccess = onLogoutSuccess
        let vm = KoinHelper().getProfileViewModel()
        _viewModel = State(initialValue: vm)
        _uiState = State(initialValue: vm.uiState.value)
    }

    var body: some View {
        ProfileView(
            displayName: uiState.user?.displayName,
            avatarUrl: uiState.user?.avatarUrl,
            isPremium: uiState.isPremium,
            availableGenres: PlaylistPreferences.companion.AVAILABLE_GENRES,
            selectedGenres: Set(uiState.preferences.genres),
            weatherEnabled: uiState.preferences.weatherEnabled,
            excludeRemixesAndCovers: uiState.preferences.excludeRemixesAndCovers,
            excludeExplicit: uiState.preferences.excludeExplicit,
            onToggleGenre: { genre in
                viewModel.toggleGenre(genre: genre)
            },
            onWeatherToggle: { enabled in
                viewModel.setWeatherEnabled(enabled: enabled)
            },
            onExcludeRemixesToggle: { exclude in
                viewModel.setExcludeRemixesAndCovers(exclude: exclude)
            },
            onExcludeExplicitToggle: { exclude in
                viewModel.setExcludeExplicit(exclude: exclude)
            },
            onLogoutConfirm: {
                viewModel.logout()
            }
        )
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
            }
        }
        .task {
            for await effect in viewModel.effects {
                if effect is ProfileEffectNavigateToAuth {
                    onLogoutSuccess()
                }
            }
        }
    }
}
