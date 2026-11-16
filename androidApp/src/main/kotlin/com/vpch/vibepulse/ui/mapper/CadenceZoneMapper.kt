package com.vpch.vibepulse.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vpch.vibepulse.R
import com.vpch.vibepulse.domain.model.CadenceZone

@Composable
fun CadenceZone.toLabel(): String = stringResource(
    when (this) {
        CadenceZone.IDLE -> R.string.cadence_zone_idle
        CadenceZone.WALKING -> R.string.cadence_zone_walking
        CadenceZone.EASY_RUN -> R.string.cadence_zone_easy_run
        CadenceZone.TEMPO_RUN -> R.string.cadence_zone_tempo_run
        CadenceZone.SPRINT -> R.string.cadence_zone_sprint
    }
)