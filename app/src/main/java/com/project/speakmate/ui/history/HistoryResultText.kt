package com.project.speakmate.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.history.state.HistoryResultPresentation

@Composable
fun historyResultText(result: HistoryResultPresentation): String = stringResource(when (result) {
    HistoryResultPresentation.PASSED -> R.string.placement_passed
    HistoryResultPresentation.PARTIAL -> R.string.unit_result_partial
    HistoryResultPresentation.NOT_PASSED -> R.string.placement_not_passed
    HistoryResultPresentation.NOT_EVALUATED -> R.string.unit_result_insufficient
    HistoryResultPresentation.COMPLETED -> R.string.history_completed
})
