package com.vpch.vibepulse.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpch.vibepulse.domain.repository.LocalActivityRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.domain.usecase.GetActivityDetailsUseCase
import com.vpch.vibepulse.domain.usecase.GetStatisticsSummaryUseCase
import com.vpch.vibepulse.domain.usecase.StatisticsPeriod
import com.vpch.vibepulse.storage.SubscriptionManager
import com.vpch.vibepulse.storage.SubscriptionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val userRepository: UserRepository,
    private val localActivityRepository: LocalActivityRepository,
    private val getStatisticsSummary: GetStatisticsSummaryUseCase,
    private val getActivityDetails: GetActivityDetailsUseCase,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 10
    }

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val selectedPeriod = MutableStateFlow(StatisticsPeriod.WEEK)
    private val sortOrder = MutableStateFlow(ActivitySortOrder.NEWEST_FIRST)
    private val visibleLimit = MutableStateFlow(PAGE_SIZE)

    init {
        subscriptionManager.refresh()
        observeUserStatistics()
    }

    fun onIntent(intent: StatisticsIntent) {
        when (intent) {
            StatisticsIntent.ActivateSubscription -> {
                subscriptionManager.activateSubscription()
            }
            is StatisticsIntent.SelectPeriod -> {
                selectedPeriod.value = intent.period
                visibleLimit.value = PAGE_SIZE
            }
            is StatisticsIntent.SelectSortOrder -> {
                sortOrder.value = intent.sortOrder
                visibleLimit.value = PAGE_SIZE
            }
            StatisticsIntent.LoadMoreActivities -> {
                visibleLimit.update { it + PAGE_SIZE }
            }
            is StatisticsIntent.OpenActivityDetails -> loadActivityDetails(intent.activityId)
            StatisticsIntent.CloseActivityDetails -> {
                updateSuccessState {
                    it.copy(selectedActivityDetails = null, isDetailsLoading = false)
                }
            }
            is StatisticsIntent.DeleteActivity -> deleteActivity(intent.activityId)
        }
    }

    private fun observeUserStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading

            userRepository.getCurrentUser()
                .onSuccess { user ->
                    val spotifyId = user.spotifyId
                    subscriptionManager.state
                        .flatMapLatest { subState ->
                            if (subState.status != SubscriptionStatus.ACTIVE) {
                                flowOf(StatisticsUiState.Paywall(subscriptionState = subState))
                            } else {
                                selectedPeriod.flatMapLatest { period ->
                                    combine(
                                        getStatisticsSummary(spotifyId, period),
                                        sortOrder,
                                        visibleLimit
                                    ) { periodResult, currentSort, limit ->
                                        val sorted = when (currentSort) {
                                            ActivitySortOrder.NEWEST_FIRST ->
                                                periodResult.filteredActivities.sortedByDescending { it.startTime }
                                            ActivitySortOrder.OLDEST_FIRST ->
                                                periodResult.filteredActivities.sortedBy { it.startTime }
                                            ActivitySortOrder.MOST_STEPS ->
                                                periodResult.filteredActivities.sortedByDescending { it.totalSteps }
                                            ActivitySortOrder.HIGHEST_CADENCE ->
                                                periodResult.filteredActivities.sortedByDescending { it.avgCadence }
                                        }

                                        val currentSuccess = _uiState.value as? StatisticsUiState.Success
                                        StatisticsUiState.Success(
                                            subscriptionState = subState,
                                            selectedPeriod = period,
                                            sortOrder = currentSort,
                                            summary = periodResult.summary,
                                            activities = sorted.take(limit),
                                            totalFilteredCount = sorted.size,
                                            hasMoreActivities = sorted.size > limit,
                                            selectedActivityDetails = currentSuccess?.selectedActivityDetails,
                                            isDetailsLoading = currentSuccess?.isDetailsLoading ?: false
                                        )
                                    }
                                }
                            }
                        }
                        .catch { error ->
                            _uiState.value = StatisticsUiState.Error(
                                message = error.message ?: "Помилка завантаження статистики"
                            )
                        }
                        .collect { newState ->
                            _uiState.value = newState
                        }
                }
                .onFailure { error ->
                    _uiState.value = StatisticsUiState.Error(
                        message = error.message ?: "Не вдалося визначити користувача"
                    )
                }
        }
    }

    private fun loadActivityDetails(activityId: Long) {
        viewModelScope.launch {
            updateSuccessState { it.copy(isDetailsLoading = true) }
            val details = getActivityDetails(activityId)
            updateSuccessState {
                it.copy(
                    selectedActivityDetails = details,
                    isDetailsLoading = false
                )
            }
        }
    }

    private fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            localActivityRepository.deleteActivity(activityId)
            updateSuccessState { state ->
                if (state.selectedActivityDetails?.details?.activity?.id == activityId) {
                    state.copy(selectedActivityDetails = null)
                } else {
                    state
                }
            }
        }
    }

    private inline fun updateSuccessState(
        crossinline transform: (StatisticsUiState.Success) -> StatisticsUiState.Success
    ) {
        _uiState.update { current ->
            if (current is StatisticsUiState.Success) transform(current) else current
        }
    }
}