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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.ui.history.historyResultText
import com.project.speakmate.ui.history.rememberHistoryTime
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun HistorySessionCard(session: HistorySessionUiState, onOpenSession: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberHistoryTime(session.startedAtMillis)
    val typeLabel = stringResource(when (session.practiceType) {
        PracticeType.FREE_PRACTICE -> R.string.history_type_free_practice
        PracticeType.GOAL_ASSESSMENT -> R.string.history_filter_goal
        else -> R.string.history_filter_lessons
    })
    val metadata = if (session.practiceType == null) session.metadata else stringResource(
        R.string.history_runtime_metadata, typeLabel, time, session.duration
    )
    val resultLabel = if (session.practiceType == null) session.resultLabel else historyResultText(session.result)
    val statusColor = when (session.result) {
        HistoryResultPresentation.PASSED -> DesignColor.Success
        HistoryResultPresentation.PARTIAL -> DesignColor.ResultWarning
        HistoryResultPresentation.NOT_PASSED -> DesignColor.Error
        else -> DesignColor.Muted
    }
    val card = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface).clickable(onClick = onOpenSession).padding(16.dp)
    if (LocalDensity.current.fontScale > 1f) {
        Column(card, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HistorySessionDescription(session.title.ifBlank { stringResource(R.string.history_content_unavailable) }, metadata)
            Text(resultLabel, style = DesignTypography.Badge, color = statusColor,
                modifier = Modifier.background(statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA),
                    RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    } else Row(card, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(when (session.practiceType) {
            PracticeType.FREE_PRACTICE -> R.drawable.ic_training_play
            PracticeType.GOAL_ASSESSMENT -> R.drawable.ic_training_flag
            else -> R.drawable.ic_training_preparation_vocabulary
        }), null, Modifier.size(24.dp), tint = DesignColor.Primary)
        HistorySessionDescription(session.title.ifBlank { stringResource(R.string.history_content_unavailable) }, metadata, Modifier.weight(1f))
        Text(resultLabel, style = DesignTypography.Badge, color = statusColor,
            modifier = Modifier.background(statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun HistorySessionDescription(title: String, metadata: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(metadata, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}
