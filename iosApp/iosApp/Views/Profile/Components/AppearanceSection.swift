import SwiftUI

struct AppearanceSection: View {
    @Environment(\.appTheme) private var colors
    @Binding var selectedThemeRaw: String

    var body: some View {
        Section {
            Picker("profile_theme_title", selection: $selectedThemeRaw) {
                ForEach(AppThemePreference.allCases) { theme in
                    Text(theme.titleKey).tag(theme.rawValue)
                }
            }
            .pickerStyle(.segmented)
        } header: {
            Text("profile_theme_title")
                .foregroundStyle(colors.textSecondary)
        }
    }
}
