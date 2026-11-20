import SwiftUI
import SharedLogic

struct ActivityRowView: View {
    @Environment(\.appTheme) private var colors
    let activity: Activity

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(Date(timeIntervalSince1970: TimeInterval(activity.startTime.epochSeconds)), style: .date)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                if let weather = activity.weather {
                    Text("\(Int(weather.temperature.rounded()))°C \(weather.condition)")
                        .font(.caption2)
                        .foregroundStyle(colors.textSecondary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(colors.buttonSecondary, in: Capsule())
                }
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(colors.iconSecondary)
            }

            HStack {
                statMini(title: "stats_metric_steps", value: "\(activity.totalSteps)")
                Spacer()
                statMini(title: "stats_metric_time", value: formatSecondsToClock(activity.activeDurationSeconds))
                Spacer()
                statMini(title: "stats_metric_avg_cadence", value: "\(Int(activity.avgCadence.rounded())) SPM")
                Spacer()
                statMini(title: "stats_metric_max_cadence", value: "\(Int(activity.maxCadence.rounded()))")
            }
        }
        .padding(.vertical, 4)
    }

    private func statMini(title: LocalizedStringKey, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(colors.textSecondary)
            Text(value)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()
        }
    }
}
