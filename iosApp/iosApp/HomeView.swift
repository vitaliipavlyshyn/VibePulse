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
               s.weather == nil {
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

    @Environment(\.appTheme) private var colors

    var body: some View {
        VStack(spacing: 12) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    HomeHeaderView(
                        user: state.user,
                        weather: state.weather,
                        isWeatherLoading: state.isWeatherLoading,
                        hasLocationPermission: state.hasLocationPermission,
                        isLocationEnabled: state.isLocationEnabled,
                        onRefreshWeather: { onIntent(HomeIntentRefreshWeather()) }
                    )

                    CadenceTrackerCardView(
                        runStatus: state.runStatus,
                        currentCadence: Int(state.currentCadence),
                        cadenceZone: state.cadenceZone
                    )

                    SpotifyPlayerCardView(
                        track: state.currentTrack,
                        isPlaying: state.isPlaying,
                        isLiked: state.isTrackLiked,
                        positionMs: state.trackPositionMs,
                        durationMs: state.trackDurationMs,
                        onToggleLike: { onIntent(HomeIntentToggleLikeClicked()) },
                        onTogglePlayPause: { onIntent(HomeIntentTogglePlayPauseClicked()) },
                        onSkipNext: { onIntent(HomeIntentSkipNextClicked()) },
                        onSeekTo: { onIntent(HomeIntentSeekTo(positionMs: $0)) }
                    )

                    WorkoutTimerCardView(
                        runStatus: state.runStatus,
                        elapsedSeconds: state.elapsedSeconds
                    )
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 12)
            }

            RunControlsSectionView(
                runStatus: state.runStatus,
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

private struct HomeHeaderView: View {
    let user: User?
    let weather: WeatherSnapshot?
    let isWeatherLoading: Bool
    let hasLocationPermission: Bool
    let isLocationEnabled: Bool
    let onRefreshWeather: () -> Void

    @Environment(\.appTheme) private var colors

    var body: some View {
        HStack(spacing: 10) {
            // Avatar
            ZStack {
                Circle()
                    .fill(colors.buttonSecondary)
                    .overlay(
                        Circle().stroke(colors.strokePrimary.opacity(0.5), lineWidth: 1)
                    )

                Text(userInitial)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(colors.textPrimary)

                if let avatarString = user?.avatarUrl, let url = URL(string: avatarString) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image
                                .resizable()
                                .scaledToFill()
                        }
                    }
                    .clipShape(Circle())
                }
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(user?.displayName ?? String(localized: "unknown"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)

                Text(user?.hasPremium == true ? "spotify_premium" : "spotify_not_connected")
                    .font(.system(size: 11))
                    .foregroundStyle(colors.textSecondary)
            }

            Spacer(minLength: 8)

            // Weather Badge
            Button(action: onRefreshWeather) {
                HStack(spacing: 6) {
                    if isWeatherLoading {
                        ProgressView()
                            .tint(colors.indicator)
                            .scaleEffect(0.75)
                        Text("weather")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textSecondary)
                    } else if let weather {
                        if let iconStr = weather.iconUrl, let iconUrl = URL(string: iconStr) {
                            AsyncImage(url: iconUrl) { phase in
                                if let image = phase.image {
                                    image
                                        .resizable()
                                        .renderingMode(.template)
                                        .scaledToFit()
                                        .foregroundStyle(colors.indicator)
                                } else {
                                    Image(systemName: "cloud.sun.fill")
                                        .foregroundStyle(colors.indicator)
                                }
                            }
                            .frame(width: 24, height: 24)
                        } else {
                            Image(systemName: "location.fill")
                                .font(.system(size: 13))
                                .foregroundStyle(colors.indicator)
                        }

                        VStack(alignment: .leading, spacing: 1) {
                            Text("\(Int(weather.temperature.rounded()))°C · \(weather.condition)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(colors.textPrimary)

                            if !weather.area.isEmpty {
                                Text(weather.area)
                                    .font(.system(size: 10))
                                    .foregroundStyle(colors.textSecondary)
                            }
                        }
                    } else if !hasLocationPermission {
                        Image(systemName: "location.slash.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.iconSecondary)
                        Text("allow_location")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textPrimary)
                    } else if !isLocationEnabled {
                        Image(systemName: "location.slash.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.textError)
                        Text("turn_on_gps")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textError)
                    } else {
                        Image(systemName: "location.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(colors.iconSecondary)
                        Text("update_weather")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(colors.textPrimary)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(colors.buttonPrimary, in: Capsule())
                .overlay(
                    Capsule().stroke(colors.strokePrimary.opacity(0.5), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var userInitial: String {
        guard let first = user?.displayName.first else { return "V" }
        return String(first).uppercased()
    }
}

private struct CadenceTrackerCardView: View {
    let runStatus: RunStatus
    let currentCadence: Int
    let cadenceZone: CadenceZone

    @Environment(\.appTheme) private var colors

    var body: some View {
        HStack {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(currentCadence)")
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                    .contentTransition(.numericText())

                Text("cadence_unit_spm")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(colors.indicator)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 6) {
                Text(zoneLocalizedKey)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(
                        runStatus == .running
                            ? colors.buttonFilled.opacity(0.16)
                            : colors.buttonSecondary,
                        in: Capsule()
                    )

                Text(statusSubtitleKey)
                    .font(.system(size: 11))
                    .foregroundStyle(colors.textSecondary)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.55), lineWidth: 1.5)
        )
    }

    private var zoneLocalizedKey: LocalizedStringKey {
        switch cadenceZone {
        case .idle: return "cadence_zone_idle"
        case .walking: return "cadence_zone_walking"
        case .easyRun: return "cadence_zone_easy_run"
        case .tempoRun: return "cadence_zone_tempo_run"
        case .sprint: return "cadence_zone_sprint"
        }
    }

    private var statusSubtitleKey: LocalizedStringKey {
        switch runStatus {
        case .idle: return "run_status_subtitle_idle"
        case .running: return "run_status_subtitle_running"
        case .paused: return "run_status_subtitle_paused"
        }
    }
}

private struct SpotifyPlayerCardView: View {
    let track: Track?
    let isPlaying: Bool
    let isLiked: Bool
    let positionMs: Int64
    let durationMs: Int64
    let onToggleLike: () -> Void
    let onTogglePlayPause: () -> Void
    let onSkipNext: () -> Void
    let onSeekTo: (Int64) -> Void

    @Environment(\.appTheme) private var colors
    @State private var isDragging: Bool = false
    @State private var dragProgress: Double = 0

    private var totalDuration: Int64 {
        if durationMs > 0 { return durationMs }
        return track?.durationMs ?? 0
    }

    private var currentProgress: Double {
        if isDragging { return dragProgress }
        guard totalDuration > 0 else { return 0 }
        return min(max(Double(positionMs) / Double(totalDuration), 0), 1)
    }

    private var displayedPositionMs: Int64 {
        if isDragging {
            return Int64(dragProgress * Double(totalDuration))
        }
        return positionMs
    }

    var body: some View {
        VStack(spacing: 10) {
            ZStack(alignment: .topTrailing) {
                HStack {
                    Spacer()
                    ZStack {
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .fill(Color(hex: "101214"))

                        Image("spotify_logo")
                            .resizable()
                            .renderingMode(.template)
                            .scaledToFit()
                            .frame(width: 56, height: 56)
                            .foregroundStyle(Color.white.opacity(0.25))

                        if let artUrlString = track?.albumArtUrl, let url = URL(string: artUrlString) {
                            AsyncImage(url: url) { phase in
                                if let image = phase.image {
                                    image
                                        .resizable()
                                        .scaledToFill()
                                }
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        }
                    }
                    .frame(width: 184, height: 184)
                    Spacer()
                }

                if let track, track.tempo > 0 {
                    Text("\(track.tempo) BPM")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(colors.buttonSecondary, in: Capsule())
                        .overlay(
                            Capsule().stroke(colors.strokePrimary.opacity(0.4), lineWidth: 1)
                        )
                }
            }

            VStack(spacing: 2) {
                Text(track?.title ?? "Немає активного треку")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)

                Text(track?.artist ?? "Увімкніть музику у Spotify")
                    .font(Font.AppTypography.bodyMedium)
                    .foregroundStyle(colors.textSecondary)
                    .lineLimit(1)
            }
            .padding(.horizontal, 8)

            VStack(spacing: 4) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(colors.trackIndicator.opacity(0.35))
                            .frame(height: 5)

                        Capsule()
                            .fill(colors.indicator)
                            .frame(width: max(0, geo.size.width * currentProgress), height: 5)
                    }
                    .frame(height: 24)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                guard track != nil, totalDuration > 0 else { return }
                                isDragging = true
                                dragProgress = min(max(value.location.x / geo.size.width, 0), 1)
                            }
                            .onEnded { value in
                                guard track != nil, totalDuration > 0 else { return }
                                let finalFraction = min(max(value.location.x / geo.size.width, 0), 1)
                                onSeekTo(Int64(finalFraction * Double(totalDuration)))
                                isDragging = false
                            }
                    )
                }
                .frame(height: 24)

                HStack {
                    Text(formatTrackTime(displayedPositionMs))
                        .font(.system(size: 11, weight: isDragging ? .bold : .regular))
                        .foregroundStyle(isDragging ? colors.textPrimary : colors.textSecondary)

                    Spacer()

                    Text(formatTrackTime(totalDuration))
                        .font(.system(size: 11))
                        .foregroundStyle(colors.textSecondary)
                }
            }
            .padding(.horizontal, 6)

            HStack {
                Button(action: onToggleLike) {
                    Image(systemName: isLiked ? "heart.fill" : "heart")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(isLiked ? colors.indicator : colors.iconSecondary)
                        .frame(width: 44, height: 44)
                        .background(
                            isLiked ? colors.buttonFilled.opacity(0.18) : colors.buttonSecondary,
                            in: Circle()
                        )
                }
                .disabled(track == nil)

                Spacer()

                Button(action: onTogglePlayPause) {
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(colors.iconFilled)
                        .frame(width: 84, height: 48)
                        .background(colors.buttonFilled, in: Capsule())
                }

                Spacer()

                Button(action: onSkipNext) {
                    Image(systemName: "forward.end.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(colors.iconPrimary)
                        .frame(width: 44, height: 44)
                        .background(colors.buttonSecondary, in: Circle())
                }
            }
            .padding(.horizontal, 6)
        }
        .padding(14)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.55), lineWidth: 1.5)
        )
    }

    private func formatTrackTime(_ ms: Int64) -> String {
        let totalSeconds = max(0, ms) / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

private struct WorkoutTimerCardView: View {
    let runStatus: RunStatus
    let elapsedSeconds: Int64

    @Environment(\.appTheme) private var colors

    var body: some View {
        VStack(spacing: 4) {
            Text(formatElapsedTime(elapsedSeconds))
                .font(.system(size: 40, weight: .heavy, design: .rounded))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()

            Text(timerSubtitleKey)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(colors.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.55), lineWidth: 1.5)
        )
    }

    private var timerSubtitleKey: LocalizedStringKey {
        switch runStatus {
        case .idle: return "timer_status_idle"
        case .running: return "timer_status_running"
        case .paused: return "timer_status_paused"
        }
    }

    private func formatElapsedTime(_ totalSeconds: Int64) -> String {
        let safe = max(0, totalSeconds)
        let hours = safe / 3600
        let minutes = (safe % 3600) / 60
        let seconds = safe % 60
        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%02d:%02d", minutes, seconds)
        }
    }
}

private struct RunControlsSectionView: View {
    let runStatus: RunStatus
    let onStart: () -> Void
    let onPause: () -> Void
    let onResume: () -> Void
    let onFinish: () -> Void

    @Environment(\.appTheme) private var colors

    private var isActive: Bool {
        runStatus != .idle
    }

    var body: some View {
        HStack(spacing: 10) {
            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    switch runStatus {
                    case .idle: onStart()
                    case .running: onPause()
                    case .paused: onResume()
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    if runStatus != .running {
                        Image(systemName: "play.fill")
                            .font(.system(size: 16, weight: .bold))
                    }
                    Text(primaryButtonTitle)
                        .font(.system(size: 16, weight: .bold))
                }
                .foregroundStyle(colors.onButtonFilled)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(colors.buttonFilled, in: Capsule())
            }

            if isActive {
                Button {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        onFinish()
                    }
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "flag.fill")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(colors.indicator)

                        Text("finish")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(colors.textPrimary)
                    }
                    .padding(.horizontal, runStatus == .paused ? 16 : 22)
                    .frame(height: 50)
                    .background(colors.buttonPrimary, in: Capsule())
                    .overlay(
                        Capsule().stroke(colors.indicator.opacity(0.5), lineWidth: 1.5)
                    )
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
    }

    private var primaryButtonTitle: LocalizedStringKey {
        switch runStatus {
        case .idle: return "start_activity"
        case .running: return "pause"
        case .paused: return "continue_"
        }
    }
}
