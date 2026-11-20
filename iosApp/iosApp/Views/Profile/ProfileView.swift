import SwiftUI
import SharedLogic

struct ProfileView: View {
    var onLogoutSuccess: () -> Void = {}

    @Environment(\.appTheme) private var colors
    @State private var viewModel = KoinHelper().getProfileViewModel()
    @State private var uiState: ProfileUiState = ProfileUiStateLoading()
    @AppStorage("app_theme_mode") private var selectedThemeRaw: String = AppThemePreference.system.rawValue
    @State private var showLogoutConfirmation = false

    private let availableGenres = [
        "pop", "rock", "electronic", "hip-hop", "indie", "workout", "metal", "drum-and-bass"
    ]

    var body: some View {
        NavigationStack {
            Group {
                switch onEnum(of: uiState) {
                case .loading:
                    ProgressView()
                        .tint(colors.indicator)
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                case .error(let errorState):
                    ContentUnavailableView {
                        Label(errorState.message.localized(), systemImage: "person.crop.circle.badge.exclamationmark")
                    } actions: {
                        Button("home_retry") {
                            viewModel.onIntent(intent: ProfileIntentLoadProfile())
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(colors.buttonFilled)
                        .foregroundStyle(colors.onButtonFilled)
                    }
                    .foregroundStyle(colors.textSecondary)

                case .success(let success):
                    profileForm(success)
                }
            }
            .navigationTitle("profile")
            .background(colors.background.ignoresSafeArea())
        }
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
            }
        }
        .task {
            for await effect in viewModel.effects {
                switch onEnum(of: effect) {
                case .navigateToAuth:
                    onLogoutSuccess()
                case .showMessage:
                    break
                }
            }
        }
    }

    @ViewBuilder
    private func profileForm(_ state: ProfileUiStateSuccess) -> some View {
        let hasPremium = state.tier.hasPremiumAccess
        let prefs = state.preferencesState.preferences

        Form {
            ProfileHeaderSection(
                user: state.user,
                tier: state.tier,
                hasPremium: hasPremium
            )
            .listRowBackground(colors.buttonPrimary)

            MusicPreferencesSection(
                prefs: prefs,
                hasPremium: hasPremium,
                availableGenres: availableGenres,
                onToggleGenre: { genre in
                    viewModel.onIntent(intent: ProfileIntentToggleGenre(genre: genre))
                },
                onToggleWeather: { enabled in
                    viewModel.onIntent(intent: ProfileIntentToggleWeatherAdaptation(enabled: enabled))
                },
                onToggleExcludeRemixes: { exclude in
                    viewModel.onIntent(intent: ProfileIntentToggleExcludeRemixesAndCovers(exclude: exclude))
                },
                onToggleExcludeExplicit: { exclude in
                    viewModel.onIntent(intent: ProfileIntentToggleExcludeExplicit(exclude: exclude))
                }
            )
            .listRowBackground(colors.buttonPrimary)

            AppearanceSection(selectedThemeRaw: $selectedThemeRaw)
                .listRowBackground(colors.buttonPrimary)

            Section {
                Button(role: .destructive) {
                    showLogoutConfirmation = true
                } label: {
                    HStack {
                        Spacer()
                        Label("profile_logout_button", systemImage: "rectangle.portrait.and.arrow.right")
                            .fontWeight(.semibold)
                            .foregroundStyle(colors.textError)
                        Spacer()
                    }
                }
            }
            .listRowBackground(colors.buttonPrimary)
        }
        .scrollContentBackground(.hidden)
        .confirmationDialog(
            "profile_logout_dialog_title",
            isPresented: $showLogoutConfirmation,
            titleVisibility: .visible
        ) {
            Button("profile_logout_dialog_confirm", role: .destructive) {
                viewModel.onIntent(intent: ProfileIntentLogoutClicked())
            }
            Button("profile_logout_dialog_cancel", role: .cancel) {}
        } message: {
            Text("profile_logout_dialog_message")
        }
    }
}
