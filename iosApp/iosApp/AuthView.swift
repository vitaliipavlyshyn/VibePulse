//
//  AuthView.swift
//  iosApp
//
//  Created by Vitalii Pavlyshyn on 12.11.2026.
//

import SwiftUI
import SharedLogic

struct AuthView: View {
    let onLoginSuccess: () -> Void

    @Environment(\.openURL) private var openURL
    @State private var viewModel = KoinHelper().getAuthViewModel()
    @State private var uiState: AuthUiState = AuthUiStateIdle()

    var body: some View {
        VStack(spacing: 16) {
            switch onEnum(of: uiState) {
            case .loading:
                ProgressView()

            case .error(let errorState):
                Text("Error: \(errorState.message)")
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                Button("Try Again") {
                    viewModel.onIntent(intent: AuthIntentConnectSpotifyClicked())
                }
                .buttonStyle(.borderedProminent)

            case .idle, .success:
                Button("Connect Spotify") {
                    viewModel.onIntent(intent: AuthIntentConnectSpotifyClicked())
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        // SKIE: нативний збір StateFlow із автоматичним скасуванням
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
            }
        }
        // SKIE: нативний збір одноразових ефектів із Channel
        .task {
            for await effect in viewModel.effects {
                switch onEnum(of: effect) {
                case .openBrowser(let browserEffect):
                    if let url = URL(string: browserEffect.url) {
                        openURL(url)
                    }
                case .navigateToHome:
                    onLoginSuccess()
                }
            }
        }
        .onOpenURL { url in
            guard url.scheme == "vibepulse",
                  let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return }

            if let code = components.queryItems?.first(where: { $0.name == "code" })?.value {
                viewModel.onIntent(intent: AuthIntentAuthCodeReceived(code: code))
            } else if let error = components.queryItems?.first(where: { $0.name == "error" })?.value {
                viewModel.onIntent(intent: AuthIntentAuthErrorReceived(error: "Spotify error: \(error)"))
            }
        }
    
    }
}
