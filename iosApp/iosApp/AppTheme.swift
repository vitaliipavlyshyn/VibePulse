import SwiftUI

struct AppColors {
    let backgroundGradient: [Color]
    let background: Color
    let buttonPrimary: Color
    let buttonSecondary: Color
    let buttonFilled: Color
    let onButtonFilled: Color
    let strokePrimary: Color
    let iconPrimary: Color
    let iconSecondary: Color
    let iconFilled: Color
    let textPrimary: Color
    let textSecondary: Color
    let textInverse: Color
    let textError: Color
    let indicator: Color
    let trackIndicator: Color
    let title: Color
    let errorContainer: Color
}

extension AppColors {
    static let goldPrimary = Color(hex: "C89F65")
    static let goldLight = Color(hex: "DFBA7D")
    static let goldDark = Color(hex: "B0803F")

    static let lightBackground = Color(hex: "FFFFFF")
    static let lightSurface = Color(hex: "F4F4F5")
    static let lightSurfaceSecondary = Color(hex: "E4E4E7")
    static let lightOnBackground = Color(hex: "1C1B1F")
    static let lightOnBackgroundMuted = Color(hex: "6E6D72")
    static let lightOnPrimary = Color(hex: "FFFFFF")

    static let silverMetallic = Color(hex: "C2CBD6")
    static let silverSheen = Color(hex: "DCE3EC")
    static let silverSteel = Color(hex: "7E8896")
    static let silverTrack = Color(hex: "3A424E")

    static let darkBackground = Color(hex: "0D0F13")
    static let darkSurface = Color(hex: "161920")
    static let darkSurfaceSecondary = Color(hex: "212630")
    static let darkOnBackground = Color(hex: "EAEEF5")
    static let darkTextMuted = Color(hex: "919AA8")
    static let darkOnPrimary = Color(hex: "0D0F13")

    static let errorRed = Color(hex: "E04848")

    static let light = AppColors(
        backgroundGradient: [lightBackground, Color(hex: "F9F9FA"), lightSurface],
        background: lightBackground,
        buttonPrimary: lightSurface,
        buttonSecondary: lightSurfaceSecondary,
        buttonFilled: goldPrimary,
        onButtonFilled: lightOnPrimary,
        strokePrimary: Color(hex: "A1A1A6"),
        iconPrimary: lightOnBackground,
        iconSecondary: lightOnBackgroundMuted,
        iconFilled: lightOnPrimary,
        textPrimary: lightOnBackground,
        textSecondary: lightOnBackgroundMuted,
        textInverse: lightOnPrimary,
        textError: errorRed,
        indicator: goldPrimary,
        trackIndicator: goldLight,
        title: goldLight,
        errorContainer: errorRed
    )

    static let dark = AppColors(
        backgroundGradient: [Color(hex: "090B0E"), darkBackground, darkSurface],
        background: darkBackground,
        buttonPrimary: darkSurface,
        buttonSecondary: darkSurfaceSecondary,
        buttonFilled: silverMetallic,
        onButtonFilled: darkOnPrimary,
        strokePrimary: silverSteel,
        iconPrimary: darkOnBackground,
        iconSecondary: darkTextMuted,
        iconFilled: darkOnPrimary,
        textPrimary: darkOnBackground,
        textSecondary: darkTextMuted,
        textInverse: darkOnPrimary,
        textError: errorRed,
        indicator: silverMetallic,
        trackIndicator: silverTrack,
        title: silverSheen,
        errorContainer: errorRed
    )
}

extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var rgbValue: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1.0)
    }
}

private struct AppThemeKey: EnvironmentKey {
    static let defaultValue: AppColors = .light
}

extension EnvironmentValues {
    var appTheme: AppColors {
        get { self[AppThemeKey.self] }
        set { self[AppThemeKey.self] = newValue }
    }
}

struct VibePulseTheme<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        let activeColors = (colorScheme == .dark) ? AppColors.dark : AppColors.light

        content
            .environment(\.appTheme, activeColors)
    }
}
