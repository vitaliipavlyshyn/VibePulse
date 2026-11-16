//
//  ThemeSelectionCard.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI

struct ThemeSelectionCard: View {
    let selectedMode: AppThemeMode
    let onSelectTheme: (AppThemeMode) -> Void

    private let modes = AppThemeMode.allCases

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("profile_theme_title")
                .font(.title3.bold())
                .foregroundStyle(.primary)

            HStack(spacing: 3) {
                ForEach(Array(modes.enumerated()), id: \.element.id) { index, mode in
                    let isSelected = selectedMode == mode
                    let isFirst = index == 0
                    let isLast = index == modes.count - 1

                    let startRadius: CGFloat = (isSelected || isFirst) ? 50 : 8
                    let endRadius: CGFloat = (isSelected || isLast) ? 50 : 8

                    Button {
                        onSelectTheme(mode)
                    } label: {
                        Text(mode.localizedTitle)
                            .font(.subheadline.weight(isSelected ? .bold : .medium))
                            .foregroundStyle(isSelected ? Color.white : .primary)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .padding(.horizontal, 8)
                            .background(
                                UnevenRoundedRectangle(
                                    topLeadingRadius: startRadius,
                                    bottomLeadingRadius: startRadius,
                                    bottomTrailingRadius: endRadius,
                                    topTrailingRadius: endRadius,
                                    style: .continuous
                                )
                                .fill(isSelected ? Color.accentColor : Color(.tertiarySystemFill))
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(16)
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
