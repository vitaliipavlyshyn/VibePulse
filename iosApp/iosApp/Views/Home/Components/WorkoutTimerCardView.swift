//
//  WorkoutTimerCardView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct WorkoutTimerCardView: View {
    let runStatus: RunStatus
    let elapsedSeconds: Int64

    @Environment(\.appTheme) private var colors

    var body: some View {
        VStack(spacing: 4) {
            Text(formatElapsedTime(elapsedSeconds))
                .font(.system(size: 40, weight: .heavy, design: .rounded))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()

            Text(timerSubtitleKey)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(colors.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.55), lineWidth: 1.5)
        )
    }

    private var timerSubtitleKey: LocalizedStringKey {
        switch runStatus {
        case .idle: return "timer_status_idle"
        case .running: return "timer_status_running"
        case .paused: return "timer_status_paused"
        }
    }

    private func formatElapsedTime(_ totalSeconds: Int64) -> String {
        let safe = max(0, totalSeconds)
        let hours = safe / 3600
        let minutes = (safe % 3600) / 60
        let seconds = safe % 60
        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%02d:%02d", minutes, seconds)
        }
    }
}
