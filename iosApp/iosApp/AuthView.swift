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
    @Environment(\.appTheme) private var colors

    @State private var viewModel = KoinHelper().getAuthViewModel()
    @State private var uiState: AuthUiState = AuthUiStateIdle()
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            LinearGradient(
                colors: colors.backgroundGradient,
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            if case .loading = onEnum(of: uiState) {
                ProgressView()
                    .tint(colors.indicator)
                    .scaleEffect(1.2)
            } else {
                VStack {
                    Spacer()

                    Text("vibe_pulse")
                        .font(Font.AppTypography.displayLarge)
                        .foregroundStyle(colors.title)

                    Spacer()

                    Button {
                        viewModel.onIntent(intent: AuthIntentConnectSpotifyClicked())
                    } label: {
                        HStack(spacing: 10) {
                            Image("spotify_logo")
                                .resizable()
                                .renderingMode(.template)
                                .scaledToFit()
                                .frame(width: 22, height: 22)

                            Text("connect_spotify")
                                .font(Font.AppTypography.labelLarge.weight(.semibold))
                        }
                        .foregroundStyle(colors.onButtonFilled)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(colors.buttonFilled, in: Capsule())
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                }
            }
        }
        .alert(
            String(localized: "login_error_title"),
            isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )
        ) {
            Button("try_again") {
                viewModel.onIntent(intent: AuthIntentConnectSpotifyClicked())
            }
            Button("cancel", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
                if case .error(let err) = onEnum(of: state) {
                    self.errorMessage = err.message
                }
            }
        }
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

#Preview("Light") {
    VibePulseTheme {
        AuthView(onLoginSuccess: {})
    }
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    VibePulseTheme {
        AuthView(onLoginSuccess: {})
    }
    .preferredColorScheme(.dark)
}
