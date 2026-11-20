import SwiftUI

struct OpenSpotifyTrackModal: View {
    let title: String
    let artist: String
    let albumArtUrl: String?
    let metricText: String?
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    @Environment(\.appTheme) private var colors

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 14) {
                AsyncImage(url: URL(string: albumArtUrl ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color(.tertiarySystemFill))
                            .overlay {
                                Image(systemName: "music.note")
                                    .font(.title)
                                    .foregroundStyle(.secondary)
                            }
                    }
                }
                .frame(width: 96, height: 96)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: .black.opacity(0.2), radius: 8, y: 4)

                VStack(spacing: 4) {
                    Text("spotify_open_dialog_title")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(colors.textPrimary)

                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)

                    Text(artist)
                        .font(.caption)
                        .foregroundStyle(colors.textSecondary)
                        .lineLimit(1)

                    if let metricText, !metricText.isEmpty {
                        Text(metricText)
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(colors.indicator)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 3)
                            .background(colors.indicator.opacity(0.14), in: Capsule())
                            .padding(.top, 2)
                    }
                }

                VStack(spacing: 8) {
                    Button(action: onConfirm) {
                        Text("spotify_open_dialog_confirm")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(colors.onButtonFilled)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(colors.buttonFilled, in: Capsule())
                    }

                    Button("spotify_open_dialog_cancel", role: .cancel, action: onDismiss)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(colors.textSecondary)
                        .padding(.top, 2)
                }
            }
            .padding(20)
            .frame(maxWidth: 320)
            .background(
                RoundedRectangle(cornerRadius: 26, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 26, style: .continuous)
                    .stroke(colors.strokePrimary.opacity(0.25), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 20, y: 10)
            .padding(.horizontal, 24)
        }
    }
}
