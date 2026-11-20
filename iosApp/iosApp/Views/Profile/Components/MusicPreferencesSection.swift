import SwiftUI
import SharedLogic

struct MusicPreferencesSection: View {
    @Environment(\.appTheme) private var colors
    let prefs: PlaylistPreferences
    let hasPremium: Bool
    let availableGenres: [String]
    let onToggleGenre: (String) -> Void
    let onToggleWeather: (Bool) -> Void
    let onToggleExcludeRemixes: (Bool) -> Void
    let onToggleExcludeExplicit: (Bool) -> Void

    var body: some View {
        Section {
            if !hasPremium {
                Label("profile_premium_upsell_description", systemImage: "crown.fill")
                    .font(.footnote)
                    .foregroundStyle(colors.textSecondary)
            }

            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(
                        prefs.genres.isEmpty
                            ? "profile_genres_auto_tempo"
                            : LocalizedStringKey(String(format: String(localized: "profile_genres_selected_count"), prefs.genres.count))
                    )
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(colors.textPrimary)

                    Spacer()

                    if !hasPremium {
                        Text("profile_premium_only_badge")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(colors.textSecondary)
                    }
                }

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 95), spacing: 8)], spacing: 8) {
                    ForEach(availableGenres, id: \.self) { genre in
                        let isSelected = prefs.genres.contains(genre)
                        Button {
                            onToggleGenre(genre)
                        } label: {
                            Text(genre.capitalized)
                                .font(.caption.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 7)
                                .background(
                                    isSelected ? colors.buttonFilled : colors.buttonSecondary,
                                    in: Capsule()
                                )
                                .foregroundStyle(isSelected ? colors.onButtonFilled : colors.textPrimary)
                        }
                        .buttonStyle(.plain)
                        .disabled(!hasPremium)
                    }
                }
            }
            .padding(.vertical, 4)

            Toggle(isOn: Binding(
                get: { prefs.weatherEnabled },
                set: onToggleWeather
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("profile_pref_weather_title")
                        .foregroundStyle(colors.textPrimary)
                    Text("profile_pref_weather_subtitle")
                        .font(.caption)
                        .foregroundStyle(colors.textSecondary)
                }
            }
            .tint(colors.indicator)
            .disabled(!hasPremium)

            Toggle(isOn: Binding(
                get: { prefs.excludeRemixesAndCovers },
                set: onToggleExcludeRemixes
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("profile_pref_no_remixes_title")
                        .foregroundStyle(colors.textPrimary)
                    Text("profile_pref_no_remixes_subtitle")
                        .font(.caption)
                        .foregroundStyle(colors.textSecondary)
                }
            }
            .tint(colors.indicator)
            .disabled(!hasPremium)

            Toggle(isOn: Binding(
                get: { prefs.excludeExplicit },
                set: onToggleExcludeExplicit
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("profile_pref_no_explicit_title")
                        .foregroundStyle(colors.textPrimary)
                    Text("profile_pref_no_explicit_subtitle")
                        .font(.caption)
                        .foregroundStyle(colors.textSecondary)
                }
            }
            .tint(colors.indicator)
            .disabled(!hasPremium)
        } header: {
            Text("profile_music_selection_title")
                .foregroundStyle(colors.textSecondary)
        }
    }
}
