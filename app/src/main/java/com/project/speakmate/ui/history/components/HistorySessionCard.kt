package com.project.speakmate.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.project.speakmate.domain.model.PracticeType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HistorySessionCard(session: HistorySessionUiState, onOpenSession: () -> Unit, modifier: Modifier = Modifier) {
    val typeLabel = stringResource(when (session.practiceType) {
        PracticeType.FREE_PRACTICE -> R.string.history_type_free_practice
        PracticeType.GOAL_ASSESSMENT -> R.string.goal_assessment_header
        else -> R.string.history_type_unit
    })
    val metadata = if (session.practiceType == null) session.metadata else stringResource(
        R.string.history_runtime_metadata, typeLabel, session.performedAt, session.duration
    )
    val resultLabel = if (session.practiceType == null) session.resultLabel else stringResource(when (session.result) {
        HistoryResultPresentation.PASSED -> R.string.placement_passed
        HistoryResultPresentation.PARTIAL -> R.string.unit_result_partial
        HistoryResultPresentation.NOT_PASSED -> R.string.placement_not_passed
        HistoryResultPresentation.NOT_EVALUATED -> R.string.unit_result_insufficient
        HistoryResultPresentation.COMPLETED -> R.string.history_completed
    })
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface)
        .clickable(onClick = onOpenSession).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(metadata, style = DesignTypography.Detail, color = DesignColor.Muted, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_content_chevron), null, Modifier.size(20.dp, 24.dp), tint = DesignColor.Muted)
        }
        Text(session.title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(resultLabel, style = DesignTypography.Detail, color = when (session.result) {
            HistoryResultPresentation.PASSED -> DesignColor.Success
            HistoryResultPresentation.PARTIAL -> DesignColor.ResultWarning
            HistoryResultPresentation.NOT_PASSED -> DesignColor.Error
            HistoryResultPresentation.COMPLETED, HistoryResultPresentation.NOT_EVALUATED -> DesignColor.Muted
        })
    }
}
