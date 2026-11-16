//
//  MusicPreferencesCard.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI

struct MusicPreferencesCard: View {
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

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("profile_music_selection_title")
                    .font(.title3.bold())
                    .foregroundStyle(.primary)

                Spacer()

                if !isPremium {
                    Text("profile_premium_only_badge")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.accentColor)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(Color(.tertiarySystemFill)))
                } else {
                    if selectedGenres.isEmpty {
                        Text("profile_genres_auto_tempo")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.secondary)
                    } else {
                        Text(String(format: String(localized: "profile_genres_selected_count"), selectedGenres.count))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.accentColor)
                    }
                }
            }
            .padding(.horizontal, 16)

            if !isPremium {
                Text("profile_premium_upsell_description")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 16)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(availableGenres, id: \.self) { genre in
                            let isSelected = selectedGenres.contains {
                                $0.caseInsensitiveCompare(genre) == .orderedSame
                            }
                            CompactGenreChip(
                                label: genre,
                                isSelected: isSelected,
                                onTap: { onToggleGenre(genre) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                }

                Divider()
                    .padding(.horizontal, 16)

                VStack(spacing: 4) {
                    PreferenceToggleRow(
                        title: "profile_pref_weather_title",
                        subtitle: "profile_pref_weather_subtitle",
                        isOn: Binding(
                            get: { weatherEnabled },
                            set: { onWeatherToggle($0) }
                        )
                    )

                    Divider()

                    PreferenceToggleRow(
                        title: "profile_pref_no_remixes_title",
                        subtitle: "profile_pref_no_remixes_subtitle",
                        isOn: Binding(
                            get: { excludeRemixesAndCovers },
                            set: { onExcludeRemixesToggle($0) }
                        )
                    )

                    Divider()

                    PreferenceToggleRow(
                        title: "profile_pref_no_explicit_title",
                        subtitle: "profile_pref_no_explicit_subtitle",
                        isOn: Binding(
                            get: { excludeExplicit },
                            set: { onExcludeExplicitToggle($0) }
                        )
                    )
                }
                .padding(.horizontal, 16)
            }
        }
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .stroke(Color(.separator).opacity(0.35), lineWidth: 1)
        )
    }
}

private struct CompactGenreChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.68)) {
                onTap()
            }
        }) {
            Text(label)
                .font(.caption.weight(isSelected ? .bold : .medium))
                .foregroundStyle(isSelected ? Color.white : .primary)
                .padding(.horizontal, 11)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: isSelected ? 50 : 10, style: .continuous)
                        .fill(isSelected ? Color.accentColor : Color(.tertiarySystemFill))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: isSelected ? 50 : 10, style: .continuous)
                        .stroke(
                            isSelected ? Color.accentColor : Color(.separator).opacity(0.3),
                            lineWidth: 1
                        )
                )
        }
        .buttonStyle(.plain)
    }
}

private struct PreferenceToggleRow: View {
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)

                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 4)
        }
        .tint(Color.accentColor)
    }
}
