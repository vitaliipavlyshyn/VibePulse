package com.vpch.vibepulse.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.vpch.vibepulse.domain.model.AppTierConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class SubscriptionStatus {
    ACTIVE,
    NEVER_SUBSCRIBED,
    EXPIRED
}

data class SubscriptionState(
    val status: SubscriptionStatus,
    val isTrial: Boolean = false,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val daysRemaining: Int = 0
)

class SubscriptionManager(
    private val settings: Settings,
    private val appTierConfig: AppTierConfig
) {
    companion object {
        private const val KEY_HAD_SUBSCRIPTION = "sub_had_subscription"
        private const val KEY_IS_TRIAL = "sub_is_trial"
        private const val KEY_START_MS = "sub_start_epoch_ms"
        private const val KEY_END_MS = "sub_end_epoch_ms"
        private const val KEY_HISTORY = "sub_history_intervals"

        private const val TRIAL_DAYS = 14
        private const val PAID_SUBSCRIPTION_DAYS = 30
    }

    private val _state = MutableStateFlow(evaluateCurrentState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    fun refresh() {
        _state.value = evaluateCurrentState()
    }

    /**
     * Якщо підписки ще ніколи не було — активує пробний період на 14 днів.
     * Якщо тріал або попередня підписка вже були — оформлює місячну на 30 днів.
     */
    fun activateSubscription() {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val isFirstTime = !settings[KEY_HAD_SUBSCRIPTION, false]
        val durationDays = if (isFirstTime) TRIAL_DAYS else PAID_SUBSCRIPTION_DAYS
        val endMs = nowMs + durationDays.days.inWholeMilliseconds

        settings[KEY_HAD_SUBSCRIPTION] = true
        settings[KEY_IS_TRIAL] = isFirstTime
        settings[KEY_START_MS] = nowMs
        settings[KEY_END_MS] = endMs

        val existingHistory = settings[KEY_HISTORY, ""]
        val typeTag = if (isFirstTime) "TRIAL" else "MONTHLY"
        val newEntry = "$typeTag:$nowMs:$endMs"
        settings[KEY_HISTORY] = if (existingHistory.isBlank()) {
            newEntry
        } else {
            "$existingHistory,$newEntry"
        }

        _state.value = evaluateCurrentState()
    }

    private fun evaluateCurrentState(): SubscriptionState {
        val nowMs = Clock.System.now().toEpochMilliseconds()

        if (appTierConfig.isPremium) {
            val savedStart = settings[KEY_START_MS, 0L].takeIf { it > 0L } ?: nowMs
            val savedEnd = settings[KEY_END_MS, 0L].takeIf { it > nowMs }
                ?: (nowMs + PAID_SUBSCRIPTION_DAYS.days.inWholeMilliseconds)

            if (!settings[KEY_HAD_SUBSCRIPTION, false]) {
                settings[KEY_HAD_SUBSCRIPTION] = true
                settings[KEY_IS_TRIAL] = false
                settings[KEY_START_MS] = savedStart
                settings[KEY_END_MS] = savedEnd
            }

            val daysLeft = ((savedEnd - nowMs) / 1.days.inWholeMilliseconds).toInt().coerceAtLeast(1)
            return SubscriptionState(
                status = SubscriptionStatus.ACTIVE,
                isTrial = false,
                startEpochMs = savedStart,
                endEpochMs = savedEnd,
                daysRemaining = daysLeft
            )
        }

        val hadSub = settings[KEY_HAD_SUBSCRIPTION, false]
        if (!hadSub) {
            return SubscriptionState(status = SubscriptionStatus.NEVER_SUBSCRIBED)
        }

        val isTrial = settings[KEY_IS_TRIAL, false]
        val startMs = settings[KEY_START_MS, 0L].takeIf { it > 0L }
        val endMs = settings[KEY_END_MS, 0L].takeIf { it > 0L } ?: 0L

        return if (nowMs < endMs) {
            val daysLeft = ((endMs - nowMs) / 1.days.inWholeMilliseconds).toInt().coerceAtLeast(1)
            SubscriptionState(
                status = SubscriptionStatus.ACTIVE,
                isTrial = isTrial,
                startEpochMs = startMs,
                endEpochMs = endMs,
                daysRemaining = daysLeft
            )
        } else {
            SubscriptionState(
                status = SubscriptionStatus.EXPIRED,
                isTrial = isTrial,
                startEpochMs = startMs,
                endEpochMs = endMs,
                daysRemaining = 0
            )
        }
    }
}