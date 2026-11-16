//
//  CadenceTrackerCardView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct CadenceTrackerCardView: View {
    let runStatus: RunStatus
    let currentCadence: Int
    let cadenceZone: CadenceZone

    @Environment(\.appTheme) private var colors

    var body: some View {
        HStack {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(currentCadence)")
                    .font(.system(size: 44, weight: .heavy, design: .rounded))
                    .foregroundStyle(colors.textPrimary)
                    .contentTransition(.numericText())

                Text("cadence_unit_spm")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(colors.indicator)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 6) {
                Text(zoneLocalizedKey)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(colors.textPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(
                        runStatus == .running
                            ? colors.buttonFilled.opacity(0.16)
                            : colors.buttonSecondary,
                        in: Capsule()
                    )

                Text(statusSubtitleKey)
                    .font(.system(size: 11))
                    .foregroundStyle(colors.textSecondary)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .background(colors.buttonPrimary, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(colors.strokePrimary.opacity(0.55), lineWidth: 1.5)
        )
    }

    private var zoneLocalizedKey: LocalizedStringKey {
        switch cadenceZone {
        case .idle: return "cadence_zone_idle"
        case .walking: return "cadence_zone_walking"
        case .easyRun: return "cadence_zone_easy_run"
        case .tempoRun: return "cadence_zone_tempo_run"
        case .sprint: return "cadence_zone_sprint"
        }
    }

    private var statusSubtitleKey: LocalizedStringKey {
        switch runStatus {
        case .idle: return "run_status_subtitle_idle"
        case .running: return "run_status_subtitle_running"
        case .paused: return "run_status_subtitle_paused"
        }
    }
}
