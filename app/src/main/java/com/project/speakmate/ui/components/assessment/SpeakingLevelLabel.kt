package com.project.speakmate.ui.components.assessment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel

@Composable
fun rememberSpeakingLevelLabel(level: SpeakingLevel): String =
    stringResource(
        when (level) {
            SpeakingLevel.A1 -> R.string.placement_a1_label
            SpeakingLevel.A2 -> R.string.placement_a2_label
            SpeakingLevel.B1 -> R.string.placement_b1_label
            SpeakingLevel.B2 -> R.string.placement_b2_label
            SpeakingLevel.NOT_ASSESSED -> R.string.profile_unassessed_label
        }
    )
