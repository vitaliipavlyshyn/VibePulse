package com.vpch.vibepulse.presentation.statistics

sealed interface StatisticsMessage {
    data object LoadError : StatisticsMessage
    data object UserIdentifyError : StatisticsMessage
    data class Custom(val text: String) : StatisticsMessage
}