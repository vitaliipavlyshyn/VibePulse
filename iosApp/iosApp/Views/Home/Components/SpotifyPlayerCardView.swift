//
//  SpotifyPlayerCardView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct SpotifyPlayerCardView: View {
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
    @State private var skipTrigger: Int = 0

    private var trackKey: String {
        guard let track else { return "empty" }
        return track.spotifyUri.isEmpty ? "\(track.title)_\(track.artist)" : track.spotifyUri
    }

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
        VStack(spacing: 12) {
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
                                        .transition(.opacity.combined(with: .scale(scale: 0.96)))
                                }
                            }
                            .id(artUrlString)
                            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        }
                    }
                    .frame(width: 184, height: 184)
                    .scaleEffect(isPlaying ? 1.0 : 0.90)
                    .shadow(
                        color: Color.black.opacity(isPlaying ? 0.28 : 0.10),
                        radius: isPlaying ? 14 : 6,
                        x: 0,
                        y: isPlaying ? 8 : 3
                    )
                    .animation(.spring(response: 0.42, dampingFraction: 0.72), value: isPlaying)
                    .animation(.easeInOut(duration: 0.3), value: trackKey)
                    Spacer()
                }

                if let track, track.tempo > 0 {
                    HStack(spacing: 6) {
                        AudioEqualizerBars(isPlaying: isPlaying, color: colors.indicator)
                        Text("\(track.tempo) BPM")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(colors.textPrimary)
                            .contentTransition(.numericText())
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(colors.buttonSecondary, in: Capsule())
                    .overlay(
                        Capsule().stroke(colors.strokePrimary.opacity(0.4), lineWidth: 1)
                    )
                    .transition(.scale.combined(with: .opacity))
                }
            }

            VStack(spacing: 3) {
                Text(track?.title ?? "Немає активного треку")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                    .id("title_\(trackKey)")
                    .transition(.push(from: .bottom).combined(with: .opacity))

                Text(track?.artist ?? "Увімкніть музику у Spotify")
                    .font(Font.AppTypography.bodyMedium)
                    .foregroundStyle(colors.textSecondary)
                    .lineLimit(1)
                    .id("artist_\(trackKey)")
                    .transition(.push(from: .bottom).combined(with: .opacity))
            }
            .padding(.horizontal, 8)
            .clipped()
            .animation(.spring(response: 0.38, dampingFraction: 0.82), value: trackKey)

            VStack(spacing: 4) {
                GeometryReader { geo in
                    let barHeight: CGFloat = isDragging ? 8 : 5
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(colors.trackIndicator.opacity(0.35))
                            .frame(height: barHeight)

                        Capsule()
                            .fill(colors.indicator)
                            .frame(width: max(0, geo.size.width * currentProgress), height: barHeight)
                    }
                    .frame(height: 24)
                    .contentShape(Rectangle())
                    .animation(.spring(response: 0.25, dampingFraction: 0.75), value: isDragging)
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
                        .monospacedDigit()
                        .contentTransition(.numericText())

                    Spacer()

                    Text(formatTrackTime(totalDuration))
                        .font(.system(size: 11))
                        .foregroundStyle(colors.textSecondary)
                        .monospacedDigit()
                }
            }
            .padding(.horizontal, 6)

            HStack {
                Button(action: onToggleLike) {
                    Image(systemName: isLiked ? "heart.fill" : "heart")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(isLiked ? colors.indicator : colors.iconSecondary)
                        .symbolEffect(.bounce, value: isLiked)
                        .frame(width: 44, height: 44)
                        .background(
                            isLiked ? colors.buttonFilled.opacity(0.18) : colors.buttonSecondary,
                            in: Circle()
                        )
                        .animation(.easeInOut(duration: 0.2), value: isLiked)
                }
                .buttonStyle(PressScaleButtonStyle())
                .disabled(track == nil)

                Spacer()

                Button(action: onTogglePlayPause) {
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(colors.iconFilled)
                        .contentTransition(.symbolEffect(.replace))
                        .frame(width: 84, height: 48)
                        .background(colors.buttonFilled, in: Capsule())
                }
                .buttonStyle(PressScaleButtonStyle())

                Spacer()

                Button {
                    skipTrigger += 1
                    onSkipNext()
                } label: {
                    Image(systemName: "forward.end.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(colors.iconPrimary)
                        .symbolEffect(.bounce, value: skipTrigger)
                        .frame(width: 44, height: 44)
                        .background(colors.buttonSecondary, in: Circle())
                }
                .buttonStyle(PressScaleButtonStyle())
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

private struct PressScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.91 : 1.0)
            .animation(.spring(response: 0.24, dampingFraction: 0.68), value: configuration.isPressed)
    }
}

private struct AudioEqualizerBars: View {
    let isPlaying: Bool
    let color: Color

    @State private var animate = false

    var body: some View {
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(0..<3, id: \.self) { index in
                Capsule()
                    .fill(color)
                    .frame(width: 2.5, height: barHeight(for: index))
                    .animation(
                        isPlaying
                            ? .easeInOut(duration: 0.42 + Double(index) * 0.11).repeatForever(autoreverses: true)
                            : .easeOut(duration: 0.25),
                        value: animate
                    )
            }
        }
        .frame(height: 11)
        .onAppear {
            if isPlaying { animate = true }
        }
        .onChange(of: isPlaying) { _, playing in
            animate = playing
        }
    }

    private func barHeight(for index: Int) -> CGFloat {
        guard isPlaying, animate else {
            return [4, 7, 5][index]
        }
        return [11, 5, 10][index]
    }
}
