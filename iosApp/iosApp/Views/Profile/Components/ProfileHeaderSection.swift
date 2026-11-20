import SwiftUI
import SharedLogic

struct ProfileHeaderSection: View {
    @Environment(\.appTheme) private var colors
    let user: User?
    let tier: UserTier
    let hasPremium: Bool

    var body: some View {
        Section {
            HStack(spacing: 14) {
                AsyncImage(url: URL(string: user?.avatarUrl ?? "")) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Circle()
                        .fill(colors.buttonFilled.opacity(0.2))
                        .overlay {
                            Text(String(user?.displayName.prefix(1) ?? "V").uppercased())
                                .font(.title2.bold())
                                .foregroundStyle(colors.indicator)
                        }
                }
                .frame(width: 60, height: 60)
                .clipShape(Circle())

                VStack(alignment: .leading, spacing: 4) {
                    Text(user?.displayName ?? String(localized: "profile_default_name"))
                        .font(.headline)
                        .foregroundStyle(colors.textPrimary)

                    Text(tierBadgeKey(for: tier))
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 3)
                        .background(
                            hasPremium ? colors.buttonFilled.opacity(0.2) : colors.buttonSecondary,
                            in: Capsule()
                        )
                        .foregroundStyle(hasPremium ? colors.indicator : colors.textSecondary)
                }
            }
            .padding(.vertical, 4)
        }
    }

    private func tierBadgeKey(for tier: UserTier) -> LocalizedStringKey {
        switch tier {
        case .premium: return "profile_tier_premium"
        case .trial: return "profile_tier_trial"
        default: return "profile_tier_free"
        }
    }
}
