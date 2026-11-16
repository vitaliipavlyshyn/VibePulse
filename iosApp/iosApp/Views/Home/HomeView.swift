//
//  HomeView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 15.11.2026.
//

import SwiftUI
import UIKit
import SharedLogic

struct HomeView: View {
    let onSessionExpired: () -> Void

    @Environment(\.appTheme) private var colors
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL

    @State private var viewModel = KoinHelper().getHomeViewModel()
    @State private var uiState: HomeUiState = HomeUiStateLoading()
    @State private var toastMessage: String?

    var body: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: colors.backgroundGradient,
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            switch onEnum(of: uiState) {
            case .loading:
                ProgressView()
                    .tint(colors.indicator)
                    .scaleEffect(1.2)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

            case .error(let errorState):
                VStack(spacing: 16) {
                    Text(errorState.message)
                        .font(Font.AppTypography.bodyLarge)
                        .foregroundStyle(colors.textError)
                        .multilineTextAlignment(.center)

                    Button {
                        viewModel.onIntent(intent: HomeIntentLoadInitialData())
                    } label: {
                        Label("try_again", systemImage: "arrow.clockwise")
                            .font(Font.AppTypography.labelLarge.weight(.semibold))
                            .foregroundStyle(colors.onButtonFilled)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(colors.buttonFilled, in: Capsule())
                    }

                    Button("Log Out") {
                        viewModel.onIntent(intent: HomeIntentLogoutClicked())
                    }
                    .font(Font.AppTypography.bodyMedium)
                    .foregroundStyle(colors.textSecondary)
                }
                .padding(24)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            case .success(let successState):
                HomeSuccessContent(
                    state: successState,
                    onIntent: { viewModel.onIntent(intent: $0) }
                )
            }

            if let message = toastMessage {
                Text(message)
                    .font(Font.AppTypography.bodyMedium.weight(.medium))
                    .foregroundStyle(colors.textPrimary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.ultraThinMaterial, in: Capsule())
                    .overlay(
                        Capsule()
                            .stroke(colors.strokePrimary.opacity(0.4), lineWidth: 1)
                    )
                    .padding(.bottom, 16)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
            }
        }
        .task {
            for await effect in viewModel.effects {
                switch onEnum(of: effect) {
                case .requestLocationPermission:
                    viewModel.onIntent(intent: HomeIntentRefreshWeather())

                case .openLocationSettings:
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        openURL(url)
                    }

                case .navigateToAuth:
                    onSessionExpired()

                case .showMessage(let msgEffect):
                    showToast(msgEffect.message)
                }
            }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active,
               case .success(let s) = onEnum(of: uiState),
               s.weather.snapshot == nil {
                viewModel.onIntent(intent: HomeIntentRefreshWeather())
            }
        }
    }

    private func showToast(_ text: String) {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            toastMessage = text
        }
        Task {
            try? await Task.sleep(for: .seconds(2.5))
            await MainActor.run {
                withAnimation(.easeOut(duration: 0.25)) {
                    if toastMessage == text {
                        toastMessage = nil
                    }
                }
            }
        }
    }
}

private struct HomeSuccessContent: View {
    let state: HomeUiStateSuccess
    let onIntent: (HomeIntent) -> Void

    var body: some View {
        VStack(spacing: 12) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    HomeHeaderView(
                        user: state.user,
                        weather: state.weather.snapshot,
                        isWeatherLoading: state.weather.isLoading,
                        hasLocationPermission: state.weather.hasPermission,
                        isLocationEnabled: state.weather.isLocationEnabled,
                        onRefreshWeather: { onIntent(HomeIntentRefreshWeather()) }
                    )

                    CadenceTrackerCardView(
                        runStatus: state.run.status,
                        currentCadence: Int(state.run.currentCadence),
                        cadenceZone: state.run.cadenceZone
                    )

                    SpotifyPlayerCardView(
                        track: state.player.currentTrack,
                        isPlaying: state.player.isPlaying,
                        isLiked: state.player.isLiked,
                        positionMs: state.player.positionMs,
                        durationMs: state.player.durationMs,
                        onToggleLike: { onIntent(HomeIntentToggleLikeClicked()) },
                        onTogglePlayPause: { onIntent(HomeIntentTogglePlayPauseClicked()) },
                        onSkipNext: { onIntent(HomeIntentSkipNextClicked()) },
                        onSeekTo: { onIntent(HomeIntentSeekTo(positionMs: $0)) }
                    )

                    WorkoutTimerCardView(
                        runStatus: state.run.status,
                        elapsedSeconds: state.run.elapsedSeconds
                    )
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 12)
            }

            RunControlsSectionView(
                runStatus: state.run.status,
                onStart: { onIntent(HomeIntentStartRunClicked()) },
                onPause: { onIntent(HomeIntentPauseRunClicked()) },
                onResume: { onIntent(HomeIntentResumeRunClicked()) },
                onFinish: { onIntent(HomeIntentFinishRunClicked()) }
            )
            .padding(.horizontal, 16)
            .padding(.bottom, 10)
        }
    }
}
