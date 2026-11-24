package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeType

@Composable
fun rememberHistoryPracticeTypeLabel(type: PracticeType?): String =
    stringResource(
        when (type) {
            PracticeType.FREE_PRACTICE -> R.string.history_type_free_practice
            PracticeType.GOAL_ASSESSMENT -> R.string.goal_assessment_header
            PracticeType.TRAINING_UNIT -> R.string.history_type_unit
            null -> R.string.history_recovery_title
        }
    )
