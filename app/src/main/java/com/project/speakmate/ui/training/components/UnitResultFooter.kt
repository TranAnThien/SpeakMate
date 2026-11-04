package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun UnitResultFooter(
    isPassed: Boolean,
    onContinuePath: () -> Unit,
    onReplayUnit: () -> Unit,
    onRetryUnit: () -> Unit,
    onReviewLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(stringResource(if (isPassed) R.string.home_continue_path else R.string.unit_result_retry),
            if (isPassed) onContinuePath else onRetryUnit)
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(DesignColor.SecondaryButton).clickable(onClick = if (isPassed) onReplayUnit else onReviewLesson)
            .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(if (isPassed) R.string.unit_result_replay else R.string.unit_result_review),
                style = DesignTypography.Button, color = DesignColor.Primary, textAlign = TextAlign.Center)
        }
    }
}
