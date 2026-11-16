//
//  ProfileHeaderSection.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI

struct ProfileHeaderSection: View {
    let displayName: String?
    let avatarUrl: String?
    let isPremium: Bool

    private var initialLetter: String {
        guard let first = displayName?.trimmingCharacters(in: .whitespacesAndNewlines).first else {
            return "V"
        }
        return String(first).uppercased()
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(Color(.secondarySystemGroupedBackground))

                Text(initialLetter)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.primary)

                if let avatarUrl, let url = URL(string: avatarUrl) {
                    AsyncImage(url: url) { phase in
                        if case .success(let image) = phase {
                            image
                                .resizable()
                                .scaledToFill()
                        }
                    }
                }
            }
            .frame(width: 84, height: 84)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color.accentColor.opacity(0.7), lineWidth: 2)
            )

            Spacer().frame(height: 12)

            if let displayName, !displayName.isEmpty {
                Text(displayName)
                    .font(.title2.bold())
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            } else {
                Text("profile_default_name")
                    .font(.title2.bold())
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }

            Spacer().frame(height: 6)

            Text(isPremium ? "profile_tier_premium" : "profile_tier_free")
                .font(.caption2.weight(.heavy))
                .foregroundStyle(isPremium ? Color.white : .secondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(
                    Capsule()
                        .fill(isPremium ? Color.accentColor : Color(.tertiarySystemFill))
                )
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }
}
