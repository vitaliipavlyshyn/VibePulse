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
