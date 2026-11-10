//
//  FontTheme.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 10.11.2026.
//

import SwiftUI

extension Font {
    static let manropeVariable = "manrope_variablefont_wght"
    static let interVariable = "inter_variablefont_opsz_wght"

    struct AppTypography {
        static let displayLarge = Font.custom(interVariable, size: 57).weight(.bold)
        static let headlineLarge = Font.custom(interVariable, size: 32).weight(.semibold)
        
        static let bodyLarge = Font.custom(manropeVariable, size: 16)
        static let bodyMedium = Font.custom(manropeVariable, size: 14)
        static let labelLarge = Font.custom(manropeVariable, size: 14)
    }
}
