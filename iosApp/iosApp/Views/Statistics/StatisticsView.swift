import SwiftUI
import Charts
import SharedLogic

struct StatisticsView: View {
    @Environment(\.appTheme) private var colors
    @State private var viewModel = KoinHelper().getStatisticsViewModel()
    @State private var uiState: StatisticsUiState = StatisticsUiStateLoading()

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
                        Label(errorState.message.localized(), systemImage: "chart.bar.xaxis")
                    }
                    .foregroundStyle(colors.textSecondary)

                case .paywall(let paywallState):
                    StatisticsPaywallView(
                        subscriptionState: paywallState.subscriptionState,
                        onActivate: { viewModel.onIntent(intent: StatisticsIntentActivateSubscription()) }
                    )

                case .success(let success):
                    statisticsSuccessContent(success)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .background(colors.background.ignoresSafeArea())
        }
        .task {
            for await state in viewModel.uiState {
                self.uiState = state
            }
        }
    }

    private func formatShortDay(date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.dateFormat = "dd.MM"
        return formatter.string(from: date)
    }

    @ViewBuilder
    private func statisticsSuccessContent(_ state: StatisticsUiStateSuccess) -> some View {
        List {
            Section {
                HStack(alignment: .center) {
                    Text("stats_title")
                        .font(.largeTitle.weight(.bold))
                        .foregroundStyle(colors.textPrimary)

                    Spacer()

                    if let endEpochMs = state.subscriptionState.endEpochMs {
                        let endDate = Date(timeIntervalSince1970: TimeInterval(endEpochMs) / 1000)
                        let formatKey = state.subscriptionState.isTrial
                            ? "stats_trial_until"
                            : "stats_premium_until"
                        let formatted = String(
                            format: String(localized: String.LocalizationValue(formatKey)),
                            formatShortDay(date: endDate)
                        )

                        Text(formatted)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(colors.indicator)
                    }
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 8, leading: 4, bottom: 4, trailing: 4))
            }

            Section {
                Picker("Period", selection: Binding(
                    get: { state.overview.selectedPeriod },
                    set: { viewModel.onIntent(intent: StatisticsIntentSelectPeriod(period: $0)) }
                )) {
                    Text("stats_period_week").tag(StatisticsPeriod.week)
                    Text("stats_period_month").tag(StatisticsPeriod.month)
                    Text("stats_period_all_time").tag(StatisticsPeriod.allTime)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            Section {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    KpiStatCard(
                        title: "stats_kpi_total_steps",
                        value: "\(state.overview.summary.totalSteps)",
                        subtitle: "stats_kpi_selected_period",
                        icon: "figure.walk"
                    )
                    KpiStatCard(
                        title: "stats_kpi_runs",
                        value: "\(state.overview.summary.totalRuns)",
                        subtitle: "\(state.overview.summary.totalDurationSeconds / 60) min",
                        icon: "flame.fill"
                    )
                    KpiStatCard(
                        title: "stats_kpi_avg_cadence",
                        value: "\(state.overview.summary.overallAvgCadence)",
                        subtitle: "stats_kpi_spm_unit",
                        icon: "metronome.fill"
                    )
                    KpiStatCard(
                        title: "stats_kpi_record_cadence",
                        value: "\(state.overview.summary.bestMaxCadence)",
                        subtitle: "stats_kpi_max_spm",
                        icon: "trophy.fill"
                    )
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            // Графік активності (Apple Charts)
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    Text(chartTitle(for: state.overview.selectedPeriod))
                        .font(.headline)
                        .foregroundStyle(colors.textPrimary)

                    if state.overview.summary.dailyStats.isEmpty {
                        Text("stats_chart_empty")
                            .font(.subheadline)
                            .foregroundStyle(colors.textSecondary)
                            .frame(maxWidth: .infinity, minHeight: 140)
                    } else {
                        Chart(state.overview.summary.dailyStats, id: \.dayStartEpochMs) { day in
                            let date = Date(timeIntervalSince1970: TimeInterval(day.dayStartEpochMs) / 1000)
                            BarMark(
                                x: .value("Day", date, unit: .day),
                                y: .value("Steps", day.totalSteps)
                            )
                            .foregroundStyle(colors.indicator.gradient)
                            .cornerRadius(6)
                        }
                        .frame(height: 170)
                    }
                }
                .padding(.vertical, 6)
            }
            .listRowBackground(colors.buttonSecondary)

            Section {
                if state.history.activities.isEmpty {
                    ContentUnavailableView {
                        Label("stats_empty_title", systemImage: "figure.run")
                    } description: {
                        Text("stats_empty_subtitle")
                    }
                    .foregroundStyle(colors.textSecondary)
                } else {
                    ForEach(state.history.activities, id: \.id) { activity in
                        Button {
                            viewModel.onIntent(intent: StatisticsIntentOpenActivityDetails(activityId: activity.id))
                        } label: {
                            ActivityRowView(activity: activity)
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                viewModel.onIntent(intent: StatisticsIntentDeleteActivity(activityId: activity.id))
                            } label: {
                                Label("stats_details_delete", systemImage: "trash")
                            }
                        }
                    }

                    if state.history.hasMoreActivities {
                        let remaining = Int(state.history.totalFilteredCount) - state.history.activities.count
                        Button {
                            viewModel.onIntent(intent: StatisticsIntentLoadMoreActivities())
                        } label: {
                            Text(String(format: String(localized: "stats_load_more"), max(0, remaining)))
                                .foregroundStyle(colors.indicator)
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
            } header: {
                HStack {
                    Text("stats_history_title")
                        .foregroundStyle(colors.textSecondary)
                    Spacer()
                    Menu {
                        ForEach(ActivitySortOrder.allCases, id: \.self) { order in
                            Button {
                                viewModel.onIntent(intent: StatisticsIntentSelectSortOrder(sortOrder: order))
                            } label: {
                                if order == state.history.sortOrder {
                                    Label(order.localizedTitle, systemImage: "checkmark")
                                } else {
                                    Text(order.localizedTitle)
                                }
                            }
                        }
                    } label: {
                        Label(state.history.sortOrder.localizedTitle, systemImage: "arrow.up.arrow.down")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(colors.indicator)
                            .textCase(nil)
                    }
                }
            }
            .listRowBackground(colors.buttonSecondary)
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .sheet(
            isPresented: Binding(
                get: { state.details.selectedDetails != nil || state.details.isLoading },
                set: { if !$0 { viewModel.onIntent(intent: StatisticsIntentCloseActivityDetails()) } }
            )
        ) {
            ActivityDetailsSheet(
                detailsState: state.details,
                onDelete: { id in
                    viewModel.onIntent(intent: StatisticsIntentDeleteActivity(activityId: id))
                }
            )
            .presentationDetents([.medium, .large])
        }
    }

    private func chartTitle(for period: StatisticsPeriod) -> LocalizedStringKey {
        switch period {
        case .week: return "stats_chart_title_week"
        case .month: return "stats_chart_title_month"
        default: return "stats_chart_title_all_time"
        }
    }
}
