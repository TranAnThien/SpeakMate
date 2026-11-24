package com.project.speakmate.ui.placement.components

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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.placement.state.PromotionStepUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PromotionStepsCard(steps: List<PromotionStepUiState>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TitleText(text = stringResource(R.string.promotion_steps_heading))
        steps
            .distinctBy { it.id }
            .forEach { step ->
                key(step.id) {
                    SpeakMateCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_training_flag),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = DesignColor.Tertiary
                            )
                            BodyText(text = step.text, modifier = Modifier.weight(1f))
                            CaptionText(
                                text = stringResource(R.string.promotion_step_caption, step.number),
                                style = DesignTypography.Badge,
                                color = DesignColor.OnWarningContainer,
                                modifier =
                                    Modifier.background(DesignColor.WarningContainer, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                            )
                        }
                    }
                }
            }
    }
}
