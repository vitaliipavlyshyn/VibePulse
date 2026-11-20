import SwiftUI
import SharedLogic

struct SpotifyPlayerCard: View {
    let player: PlayerUiState
    let onToggleLike: () -> Void
    let onTogglePlayPause: () -> Void
    let onSkipNext: () -> Void
    let onSeekTo: (Int64) -> Void
    let onOpenSpotifyTapped: () -> Void

    @Environment(\.appTheme) private var colors
    @State private var isDragging = false
    @State private var dragProgress: Double = 0

    private var totalDurationMs: Int64 {
        player.durationMs > 0 ? player.durationMs : (player.currentTrack?.durationMs ?? 0)
    }

    private var currentProgress: Double {
        guard totalDurationMs > 0 else { return 0 }
        return min(1.0, max(0.0, Double(player.positionMs) / Double(totalDurationMs)))
    }

    private var hasValidSpotifyUri: Bool {
        guard let uri = player.currentTrack?.spotifyUri else { return false }
        return !uri.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(spacing: 12) {
            ZStack(alignment: .top) {
                HStack {
                    if let tempo = player.currentTrack?.tempo, tempo > 0 {
                        Text("\(tempo) BPM")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(colors.textPrimary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(colors.buttonSecondary, in: Capsule())
                    }
                    Spacer()
                    if hasValidSpotifyUri {
                        Button(action: onOpenSpotifyTapped) {
                            Image(systemName: "arrow.up.right.circle.fill")
                                .font(.title3)
                                .foregroundStyle(.green)
                        }
                        .buttonStyle(.plain)
                    }
                }

                AsyncImage(url: URL(string: player.currentTrack?.albumArtUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(colors.buttonSecondary)
                        .overlay {
                            Image(systemName: "music.note")
                                .font(.system(size: 40))
                                .foregroundStyle(colors.iconSecondary)
                        }
                }
                .frame(width: 164, height: 164)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .scaleEffect(player.isPlaying && player.currentTrack != nil ? 1.03 : 0.97)
                .shadow(color: .black.opacity(player.isPlaying ? 0.22 : 0.08), radius: player.isPlaying ? 12 : 6, y: 5)
                .animation(.spring(response: 0.45, dampingFraction: 0.75), value: player.isPlaying)
                .padding(.top, 4)
            }

            VStack(spacing: 3) {
                Text(player.currentTrack?.title ?? String(localized: "no_active_track"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(1)
                Text(player.currentTrack?.artist ?? String(localized: "play_music_on_spotify"))
                    .font(.subheadline)
                    .foregroundStyle(colors.textSecondary)
                    .lineLimit(1)
            }

            VStack(spacing: 3) {
                Slider(
                    value: Binding(
                        get: { isDragging ? dragProgress : currentProgress },
                        set: { dragProgress = $0 }
                    ),
                    in: 0...1,
                    onEditingChanged: { editing in
                        isDragging = editing
                        if !editing {
                            onSeekTo(Int64(dragProgress * Double(totalDurationMs)))
                        }
                    }
                )
                .disabled(player.currentTrack == nil || totalDurationMs <= 0)
                .tint(colors.indicator)

                HStack {
                    Text(formatMillisToClock(isDragging ? Int64(dragProgress * Double(totalDurationMs)) : player.positionMs))
                    Spacer()
                    Text(formatMillisToClock(totalDurationMs))
                }
                .font(.caption2.monospacedDigit())
                .foregroundStyle(colors.textSecondary)
            }

            HStack(spacing: 36) {
                Button(action: onToggleLike) {
                    Image(systemName: player.isLiked ? "heart.fill" : "heart")
                        .font(.title2)
                        .foregroundStyle(player.isLiked ? .pink : colors.iconSecondary)
                        .symbolEffect(.bounce, value: player.isLiked)
                }
                .disabled(player.currentTrack == nil)

                Button(action: onTogglePlayPause) {
                    Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 52))
                        .foregroundStyle(colors.indicator)
                }

                Button(action: onSkipNext) {
                    Image(systemName: "forward.end.fill")
                        .font(.title2)
                        .foregroundStyle(colors.iconPrimary)
                }
            }
            .padding(.top, 2)
        }
        .padding(16)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.45), lineWidth: 1)
        )
    }
}
