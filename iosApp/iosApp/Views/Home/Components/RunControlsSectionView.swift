//
//  RunControlsSectionView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 16.11.2026.
//

import SwiftUI
import SharedLogic

struct RunControlsSectionView: View {
    let runStatus: RunStatus
    let onStart: () -> Void
    let onPause: () -> Void
    let onResume: () -> Void
    let onFinish: () -> Void

    @Environment(\.appTheme) private var colors

    private var isActive: Bool {
        runStatus != .idle
    }

    var body: some View {
        HStack(spacing: 10) {
            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    switch runStatus {
                    case .idle: onStart()
                    case .running: onPause()
                    case .paused: onResume()
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    if runStatus != .running {
                        Image(systemName: "play.fill")
                            .font(.system(size: 16, weight: .bold))
                    }
                    Text(primaryButtonTitle)
                        .font(.system(size: 16, weight: .bold))
                }
                .foregroundStyle(colors.onButtonFilled)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(colors.buttonFilled, in: Capsule())
            }

            if isActive {
                Button {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        onFinish()
                    }
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "flag.fill")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(colors.indicator)

                        Text("finish")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(colors.textPrimary)
                    }
                    .padding(.horizontal, runStatus == .paused ? 16 : 22)
                    .frame(height: 50)
                    .background(colors.buttonPrimary, in: Capsule())
                    .overlay(
                        Capsule().stroke(colors.indicator.opacity(0.5), lineWidth: 1.5)
                    )
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
    }

    private var primaryButtonTitle: LocalizedStringKey {
        switch runStatus {
        case .idle: return "start_activity"
        case .running: return "pause"
        case .paused: return "continue_"
        }
    }
}
