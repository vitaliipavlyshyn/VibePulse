//
//  StatisticsPaywallView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 19.11.2026.
//

import SwiftUI
import SharedLogic

struct StatisticsPaywallView: View {
    @Environment(\.appTheme) private var colors
    let subscriptionState: SubscriptionState
    let onActivate: () -> Void

    private var isExpired: Bool {
        subscriptionState.status == .expired
    }

    private var wasTrial: Bool {
        subscriptionState.isTrial
    }

    private var badgeKey: LocalizedStringKey {
        if isExpired && wasTrial {
            return "paywall_badge_trial_expired"
        } else if isExpired {
            return "paywall_badge_sub_expired"
        } else {
            return "paywall_badge_premium"
        }
    }

    private var titleKey: LocalizedStringKey {
        if isExpired && wasTrial {
            return "paywall_title_trial_expired"
        } else if isExpired {
            return "paywall_title_sub_expired"
        } else {
            return "paywall_title_default"
        }
    }

    private var buttonKey: LocalizedStringKey {
        isExpired ? "paywall_btn_renew" : "paywall_btn_try_free"
    }

    private var descriptionText: String {
        if isExpired {
            let periodLabel = String(
                localized: wasTrial ? "paywall_period_label_trial" : "paywall_period_label_monthly"
            )
            let endDateText = formatShortEndDate(epochMs: subscriptionState.endEpochMs)
            return String(
                format: String(localized: "paywall_desc_expired"),
                periodLabel,
                endDateText
            )
        } else {
            return String(localized: "paywall_desc_default")
        }
    }

    var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 20) {
                Spacer()

                Image(systemName: "chart.xyaxis.line")
                    .font(.system(size: 54))
                    .foregroundStyle(colors.indicator)

                Text(badgeKey)
                    .font(.caption.weight(.bold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(colors.buttonFilled.opacity(0.18), in: Capsule())
                    .foregroundStyle(colors.indicator)

                Text(titleKey)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .multilineTextAlignment(.center)

                Text(descriptionText)
                    .font(.subheadline)
                    .foregroundStyle(colors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)

                Button(action: onActivate) {
                    Text(buttonKey)
                        .fontWeight(.bold)
                        .foregroundStyle(colors.onButtonFilled)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(colors.buttonFilled, in: Capsule())
                }
                .padding(.horizontal, 24)

                Spacer()
            }
        }
    }

    private func formatShortEndDate(epochMs: KotlinLong?) -> String {
        guard let ms = epochMs?.int64Value else { return "" }
        let date = Date(timeIntervalSince1970: TimeInterval(ms) / 1000)
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.dateFormat = "dd.MM"
        return formatter.string(from: date)
    }
}
