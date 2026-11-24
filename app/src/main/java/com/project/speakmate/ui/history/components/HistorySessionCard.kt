package com.project.speakmate.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.history.historyResultText
import com.project.speakmate.ui.history.rememberHistoryPracticeTypeLabel
import com.project.speakmate.ui.history.rememberHistoryTime
import com.project.speakmate.ui.history.state.HistoryResultPresentation
import com.project.speakmate.ui.history.state.HistorySessionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun HistorySessionCard(session: HistorySessionUiState, onOpenSession: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberHistoryTime(session.startedAtMillis)
    val typeLabel = rememberHistoryPracticeTypeLabel(session.practiceType)
    val metadata = stringResource(R.string.history_runtime_metadata, typeLabel, time, session.duration)
    val resultLabel = historyResultText(session.result)
    val statusColor =
        when (session.result) {
            HistoryResultPresentation.PASSED -> DesignColor.Success
            HistoryResultPresentation.PARTIAL -> DesignColor.ResultWarning
            HistoryResultPresentation.NOT_PASSED -> DesignColor.Error
            else -> DesignColor.Muted
        }
    SpeakMateCard(
        modifier = modifier,
        onClick = onOpenSession,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (LocalDensity.current.fontScale > 1f) {
            HistorySessionDescription(
                session.title.ifBlank { stringResource(R.string.history_content_unavailable) },
                metadata
            )
            CaptionText(
                text = resultLabel,
                style = DesignTypography.Badge,
                color = statusColor,
                modifier =
                    Modifier.background(
                        statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA),
                        RoundedCornerShape(16.dp)
                    )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter =
                        painterResource(
                            when (session.practiceType) {
                                PracticeType.FREE_PRACTICE -> R.drawable.ic_training_play
                                PracticeType.GOAL_ASSESSMENT -> R.drawable.ic_training_flag
                                else -> R.drawable.ic_training_preparation_vocabulary
                            }
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = DesignColor.Primary
                )
                HistorySessionDescription(
                    title = session.title.ifBlank { stringResource(R.string.history_content_unavailable) },
                    metadata = metadata,
                    modifier = Modifier.weight(1f)
                )
                CaptionText(
                    text = resultLabel,
                    style = DesignTypography.Badge,
                    color = statusColor,
                    modifier =
                        Modifier.background(
                            statusColor.copy(alpha = AppConstant.STATUS_CONTAINER_ALPHA),
                            RoundedCornerShape(16.dp)
                        )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HistorySessionDescription(title: String, metadata: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TitleText(text = title)
        DetailText(text = metadata)
    }
}
