package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun UnitResultFooter(
    isPassed: Boolean,
    onContinuePath: () -> Unit,
    onReplayUnit: () -> Unit,
    onRetryUnit: () -> Unit,
    onReviewLesson: () -> Unit,
    modifier: Modifier = Modifier,
    hasSaveFailed: Boolean = false,
    onRetrySave: () -> Unit = {},
    isActionEnabled: Boolean = true
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(DesignColor.Surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryButton(
            text =
                stringResource(
                    when {
                        hasSaveFailed -> R.string.unit_result_retry_save
                        isPassed -> R.string.home_continue_path
                        else -> R.string.unit_result_retry
                    }
                ),
            onClick =
                when {
                    hasSaveFailed -> onRetrySave
                    isPassed -> onContinuePath
                    else -> onRetryUnit
                },
            enabled = isActionEnabled
        )
        SecondaryButton(
            text = stringResource(if (isPassed) R.string.unit_result_replay else R.string.unit_result_review),
            onClick = if (isPassed) onReplayUnit else onReviewLesson,
            enabled = isActionEnabled,
            isTonal = true
        )
    }
}
