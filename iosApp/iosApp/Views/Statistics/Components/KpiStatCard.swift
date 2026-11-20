import SwiftUI

struct KpiStatCard: View {
    @Environment(\.appTheme) private var colors
    let title: LocalizedStringKey
    let value: String
    let subtitle: LocalizedStringKey
    let icon: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(colors.textSecondary)
                Spacer()
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundStyle(colors.indicator)
            }
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()
            Text(subtitle)
                .font(.caption2)
                .foregroundStyle(colors.textSecondary)
        }
        .padding(12)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
