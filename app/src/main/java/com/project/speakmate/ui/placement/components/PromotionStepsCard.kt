package com.project.speakmate.ui.placement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.placement.state.PromotionStepUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PromotionStepsCard(steps: List<PromotionStepUiState>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.promotion_steps_heading), style = DesignTypography.Title, color = DesignColor.Text)
        steps.distinctBy { it.id }.forEach { step ->
            key(step.id) {
                Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_training_flag), null, Modifier.size(24.dp), tint = DesignColor.Tertiary)
                    Text(step.text, style = DesignTypography.Body, color = DesignColor.Text, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.promotion_step_caption, step.number), style = DesignTypography.Badge, color = DesignColor.OnWarningContainer,
                        modifier = Modifier.background(DesignColor.WarningContainer, RoundedCornerShape(12.dp)).padding(8.dp))
                }
            }
        }
    }
}
