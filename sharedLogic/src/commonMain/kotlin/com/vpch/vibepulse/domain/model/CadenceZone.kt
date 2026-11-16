package com.vpch.vibepulse.domain.model

enum class CadenceZone {
    IDLE,
    WALKING,
    EASY_RUN,
    TEMPO_RUN,
    SPRINT;

    companion object {
        fun fromSpm(spm: Int): CadenceZone = when {
            spm < 50 -> IDLE
            spm < 125 -> WALKING
            spm < 150 -> EASY_RUN
            spm < 175 -> TEMPO_RUN
            else -> SPRINT
        }
    }
}