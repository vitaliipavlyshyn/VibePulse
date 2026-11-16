//
//  LogoutSection.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI

struct LogoutButton: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text("profile_logout_button")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(
                    Capsule()
                        .fill(Color(.secondarySystemGroupedBackground))
                )
                .overlay(
                    Capsule()
                        .stroke(Color.red.opacity(0.35), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
