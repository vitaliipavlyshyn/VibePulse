//
//  ActivityDetailsSheet.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 19.11.2026.
//

import SwiftUI
import Charts
import UIKit
import SharedLogic

struct ActivityDetailsSheet: View {
    let detailsState: ActivityDetailsUiState
    let onDelete: (Int64) -> Void

    @Environment(\.appTheme) private var colors
    @Environment(\.openURL) private var openURL
    @State private var selectedTrackToOpen: ActivityTrack?

    var body: some View {
        NavigationStack {
            Group {
                if detailsState.isLoading {
                    ProgressView()
                        .tint(colors.indicator)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let enriched = detailsState.selectedDetails {
                    detailsContent(enriched: enriched)
                } else {
                    ContentUnavailableView(
                        "stats_details_title",
                        systemImage: "figure.run"
                    )
                }
            }
            .navigationTitle("stats_details_title")
            .navigationBarTitleDisplayMode(.inline)
        }
        .overlay {
            if let track = selectedTrackToOpen {
                let metric = track.avgCadenceDuringTrack > 0
                    ? String(format: String(localized: "stats_spm_value"), track.avgCadenceDuringTrack)
                    : nil

                OpenSpotifyTrackModal(
                    title: track.title,
                    artist: track.artist,
                    albumArtUrl: track.albumArtUrl,
                    metricText: metric,
                    onConfirm: {
                        openSpotifyTrack(uri: track.spotifyUri)
                        selectedTrackToOpen = nil
                    },
                    onDismiss: {
                        selectedTrackToOpen = nil
                    }
                )
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            }
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.82), value: selectedTrackToOpen != nil)
    }

    @ViewBuilder
    private func detailsContent(enriched: EnrichedActivityDetails) -> some View {
        let activity = enriched.details.activity
        let samples = activity.samples
        let tracks = enriched.details.tracks
        let fastestTrack = enriched.fastestTrack

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(formatFullDateTime(instant: activity.startTime))
                            .font(.subheadline.weight(.semibold))
                        Text("\(activity.totalSteps) steps · \(formatSecondsToClock(activity.activeDurationSeconds))")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    Button(role: .destructive) {
                        onDelete(activity.id)
                    } label: {
                        Label("stats_details_delete", systemImage: "trash")
                            .font(.subheadline.weight(.semibold))
                    }
                }

                if samples.count >= 2 {
                    cadenceChartSection(samples: samples)
                }

                if let fastest = fastestTrack {
                    fastestTrackCard(track: fastest)
                }

                if !tracks.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(String(format: String(localized: "stats_details_soundtrack_header"), tracks.count))
                            .font(.headline)

                        VStack(spacing: 8) {
                            ForEach(Array(tracks.enumerated()), id: \.offset) { _, track in
                                trackRow(track: track)
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }

    private func cadenceChartSection(samples: [CadenceSample]) -> some View {
        let indexedSamples = Array(samples.enumerated())

        return VStack(alignment: .leading, spacing: 8) {
            Text("stats_details_cadence_chart_title")
                .font(.subheadline.weight(.bold))

            Chart(indexedSamples, id: \.offset) { index, sample in
                LineMark(
                    x: .value("Point", index),
                    y: .value("SPM", sample.stepsPerMinute)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(colors.indicator)
                .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))

                AreaMark(
                    x: .value("Point", index),
                    y: .value("SPM", sample.stepsPerMinute)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(
                    LinearGradient(
                        colors: [colors.indicator.opacity(0.32), colors.indicator.opacity(0.0)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
            .chartXAxis(.hidden)
            .chartYAxis {
                AxisMarks(position: .leading) { value in
                    AxisGridLine()
                    AxisValueLabel {
                        if let spm = value.as(Int.self) {
                            Text("\(spm)")
                                .font(.caption2)
                        }
                    }
                }
            }
            .frame(height: 150)
        }
        .padding(14)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func fastestTrackCard(track: ActivityTrack) -> some View {
        let hasUri = !track.spotifyUri.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        return HStack(spacing: 12) {
            trackArtwork(urlString: track.albumArtUrl, size: 54, cornerRadius: 12)

            VStack(alignment: .leading, spacing: 2) {
                Text("stats_details_fastest_track_badge")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(colors.indicator)

                Text(track.title)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                Text(String(format: String(localized: "stats_details_artist_spm"), track.artist, track.avgCadenceDuringTrack))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 4)

            if hasUri {
                Button {
                    selectedTrackToOpen = track
                } label: {
                    Image(systemName: "arrow.up.right.circle.fill")
                        .font(.title2)
                        .foregroundStyle(.green)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .background(
            Color(.secondarySystemGroupedBackground),
            in: RoundedRectangle(cornerRadius: 18, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(colors.indicator.opacity(0.55), lineWidth: 1)
        )
    }

    private func trackRow(track: ActivityTrack) -> some View {
        let hasUri = !track.spotifyUri.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        return HStack(spacing: 12) {
            trackArtwork(urlString: track.albumArtUrl, size: 46, cornerRadius: 10)

            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                Text(track.artist)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            if track.avgCadenceDuringTrack > 0 {
                Text(String(format: String(localized: "stats_spm_value"), track.avgCadenceDuringTrack))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(colors.indicator)
            }

            if hasUri {
                Button {
                    selectedTrackToOpen = track
                } label: {
                    Image(systemName: "arrow.up.right.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.green)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(10)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func trackArtwork(urlString: String?, size: CGFloat, cornerRadius: CGFloat) -> some View {
        AsyncImage(url: URL(string: urlString ?? "")) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            default:
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(Color(.tertiarySystemFill))
                    .overlay {
                        Image(systemName: "music.note")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
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
}
