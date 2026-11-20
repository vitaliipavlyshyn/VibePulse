import SwiftUI
import UIKit
import SharedLogic

struct HomeView: View {
    let onSessionExpired: () -> Void
    var onNavigateToProfile: () -> Void = {}

    @Environment(\.appTheme) private var colors
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.openURL) private var openURL

    @State private var viewModel = KoinHelper().getHomeViewModel()
    @State private var uiState: HomeUiState = HomeUiStateLoading()
    @State private var toastMessage: String?
    @State private var showOpenSpotifyAlert = false

    var body: some View {
        NavigationStack {
            Group {
                switch onEnum(of: uiState) {
                case .loading:
                    ProgressView()
                        .tint(colors.indicator)
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                case .error(let errorState):
                    ContentUnavailableView {
                        Label(errorState.message.localized(), systemImage: "exclamationmark.triangle")
                    } actions: {
                        Button("home_retry") {
                            viewModel.onIntent(intent: HomeIntentLoadInitialData())
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(colors.buttonFilled)
                        .foregroundStyle(colors.onButtonFilled)

                        Button("home_relogin", role: .destructive) {
                            viewModel.onIntent(intent: HomeIntentLogoutClicked())
                        }
                        .buttonStyle(.bordered)
                    }
                    .foregroundStyle(colors.textSecondary)

                case .success(let successState):
                    homeSuccessContent(successState)
                }
            }
            .background(colors.background.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
            .overlay(alignment: .top) {
                if let toast = toastMessage {
                    Text(toast)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(colors.buttonSecondary, in: Capsule())
                        .overlay(
                            Capsule().stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
                        )
                        .padding(.top, 56)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
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
                    showToast(msgEffect.message.localized())
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

    @ViewBuilder
    private func homeSuccessContent(_ state: HomeUiStateSuccess) -> some View {
        VStack(spacing: 12) {
            headerRow(user: state.user, weather: state.weather)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    cadenceAndTimerSection(run: state.run)

                    SpotifyPlayerCard(
                        player: state.player,
                        onToggleLike: { viewModel.onIntent(intent: HomeIntentToggleLikeClicked()) },
                        onTogglePlayPause: { viewModel.onIntent(intent: HomeIntentTogglePlayPauseClicked()) },
                        onSkipNext: { viewModel.onIntent(intent: HomeIntentSkipNextClicked()) },
                        onSeekTo: { ms in viewModel.onIntent(intent: HomeIntentSeekTo(positionMs: ms)) },
                        onOpenSpotifyTapped: { showOpenSpotifyAlert = true }
                    )
                }
                .padding(.bottom, 8)
            }

            runControlsBar(runStatus: state.run.status)
                .padding(.bottom, 8)
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .alert("spotify_open_dialog_title", isPresented: $showOpenSpotifyAlert) {
            Button("spotify_open_dialog_confirm") {
                if let uri = state.player.currentTrack?.spotifyUri {
                    openSpotifyTrack(uri: uri)
                }
            }
            Button("spotify_open_dialog_cancel", role: .cancel) {}
        } message: {
            Text("spotify_open_dialog_subtitle")
        }
    }

    private func headerRow(user: User?, weather: WeatherUiState) -> some View {
        HStack(spacing: 10) {
            Button(action: onNavigateToProfile) {
                HStack(spacing: 10) {
                    AsyncImage(url: URL(string: user?.avatarUrl ?? "")) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Circle()
                            .fill(colors.buttonFilled.opacity(0.2))
                            .overlay {
                                Text(String(user?.displayName.prefix(1) ?? "V").uppercased())
                                    .font(.subheadline.bold())
                                    .foregroundStyle(colors.indicator)
                            }
                    }
                    .frame(width: 34, height: 34)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 1) {
                        Text(user?.displayName ?? String(localized: "unknown"))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(colors.textPrimary)
                            .lineLimit(1)

                        Text(user?.hasPremium == true ? "spotify_premium" : "spotify_not_connected")
                            .font(.caption2)
                            .foregroundStyle(colors.textSecondary)
                            .lineLimit(1)
                    }
                }
                .padding(.leading, 6)
                .padding(.trailing, 14)
                .padding(.vertical, 6)
                .background(colors.buttonPrimary, in: Capsule())
                .overlay(
                    Capsule().stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)

            Spacer(minLength: 6)

            weatherCapsule(weather: weather)
        }
    }

    private func weatherCapsule(weather: WeatherUiState) -> some View {
        Button {
            if !weather.hasPermission {
                viewModel.onIntent(intent: HomeIntentLocationPermissionUpdated(granted: true))
            } else {
                viewModel.onIntent(intent: HomeIntentRefreshWeather())
            }
        } label: {
            HStack(spacing: 6) {
                if weather.isLoading {
                    ProgressView()
                        .tint(colors.indicator)
                        .controlSize(.small)
                    Text("weather")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(colors.textSecondary)
                } else if let snapshot = weather.snapshot {
                    Image(systemName: "cloud.sun.fill")
                        .foregroundStyle(colors.indicator)
                    VStack(alignment: .leading, spacing: 0) {
                        Text("\(Int(snapshot.temperature.rounded()))°C · \(snapshot.condition)")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(colors.textPrimary)
                            .lineLimit(1)
                        if !snapshot.area.isEmpty {
                            Text(snapshot.area)
                                .font(.caption2)
                                .foregroundStyle(colors.textSecondary)
                                .lineLimit(1)
                        }
                    }
                } else {
                    Image(systemName: !weather.hasPermission ? "location.slash" : "location.fill")
                        .foregroundStyle(!weather.isLocationEnabled && weather.hasPermission ? colors.textError : colors.indicator)
                    Text(!weather.hasPermission ? "allow_location" : (!weather.isLocationEnabled ? "turn_on_gps" : "update_weather"))
                        .font(.caption.weight(.medium))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(colors.buttonPrimary, in: Capsule())
            .overlay(
                Capsule().stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func cadenceAndTimerSection(run: RunUiState) -> some View {
        VStack(spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text("\(run.currentCadence)")
                            .font(.system(size: 44, weight: .heavy, design: .rounded))
                            .foregroundStyle(colors.textPrimary)
                            .monospacedDigit()
                            .contentTransition(.numericText())
                        Text("cadence_unit_spm")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(colors.indicator)
                    }
                    Text(run.status.subtitleKey)
                        .font(.caption)
                        .foregroundStyle(colors.textSecondary)
                }

                Spacer()

                Text(run.cadenceZone.localizedTitle)
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        run.status == .running ? colors.buttonFilled.opacity(0.2) : colors.buttonSecondary,
                        in: Capsule()
                    )
                    .foregroundStyle(run.status == .running ? colors.indicator : colors.textPrimary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
            )

            HStack {
                Label(run.status.timerStatusKey, systemImage: "stopwatch")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(colors.textSecondary)
                Spacer()
                Text(formatSecondsToClock(run.elapsedSeconds))
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                    .monospacedDigit()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 11)
            .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
            )
        }
    }

    private func runControlsBar(runStatus: RunStatus) -> some View {
        HStack(spacing: 10) {
            Button {
                switch runStatus {
                case .idle: viewModel.onIntent(intent: HomeIntentStartRunClicked())
                case .running: viewModel.onIntent(intent: HomeIntentPauseRunClicked())
                case .paused: viewModel.onIntent(intent: HomeIntentResumeRunClicked())
                default: break
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: runStatus == .running ? "pause.fill" : "play.fill")
                    Text(runStatus == .idle ? "start_activity" : (runStatus == .running ? "pause" : "continue_"))
                        .fontWeight(.bold)
                }
                .foregroundStyle(colors.onButtonFilled)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(colors.buttonFilled, in: Capsule())
            }

            if runStatus != .idle {
                Button {
                    viewModel.onIntent(intent: HomeIntentFinishRunClicked())
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "flag.checkered")
                            .foregroundStyle(colors.indicator)
                        Text("finish")
                            .fontWeight(.semibold)
                            .foregroundStyle(colors.textPrimary)
                    }
                    .padding(.horizontal, 18)
                    .frame(height: 50)
                    .background(colors.buttonPrimary, in: Capsule())
                    .overlay(
                        Capsule().stroke(colors.indicator.opacity(0.5), lineWidth: 1.5)
                    )
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: runStatus)
    }

    private func openSpotifyTrack(uri: String) {
        let trimmed = uri.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        if let appUrl = URL(string: trimmed), UIApplication.shared.canOpenURL(appUrl) {
            openURL(appUrl)
            return
        }

        let parts = trimmed.split(separator: ":")
        if parts.count == 3, parts[0] == "spotify",
           let webUrl = URL(string: "https://open.spotify.com/\(parts[1])/\(parts[2])") {
            openURL(webUrl)
        } else if let fallbackUrl = URL(string: trimmed) {
            openURL(fallbackUrl)
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
