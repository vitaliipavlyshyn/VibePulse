//
//  AppTheme.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 10.11.2026.
//
import SwiftUI

struct AppColors {
    let primary: Color
    let background: Color
    let surface: Color
    let onBackground: Color
    let onPrimary: Color
}


extension AppColors {
    static let light = AppColors(
        primary: Color(hex: "C89F65"),
        background: Color(hex: "FFFFFF"),
        surface: Color(hex: "F4F4F5"),
        onBackground: Color(hex: "1C1B1F"),
        onPrimary: Color(hex: "FFFFFF")
    )

    static let dark = AppColors(
        primary: Color(hex: "DFBA7D"),
        background: Color(hex: "121212"),
        surface: Color(hex: "1C1C1E"),
        onBackground: Color(hex: "E6E1E5"),
        onPrimary: Color(hex: "121212")
    )
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
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

struct AppTheme {
    static var colors: AppColors {
        @Environment(\.colorScheme) var colorScheme
        return colorScheme == .dark ? .dark : .light
    }
}

struct VibePulseTheme<Content: View>: View {
    @Environment(\.colorScheme) var colorScheme
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        let activeColors = (colorScheme == .dark) ? AppColors.dark : AppColors.light

        content
            .environment(\.appTheme, activeColors)
            .preferredColorScheme(colorScheme)
    }
}
