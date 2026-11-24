package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.state.GoalAssessmentActionUiState

@Composable
fun GoalAssessmentRequiredActions(actions: List<GoalAssessmentActionUiState>, modifier: Modifier = Modifier) {
    SpeakMateCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleText(text = stringResource(R.string.goal_assessment_required_actions))
        actions.forEachIndexed { index, action ->
            key(index, action.id) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(24.dp).background(DesignColor.SecondaryButton, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_training_assessment_requirement),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = DesignColor.Primary
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailText(text = action.text, color = DesignColor.Text)
                        DetailText(text = stringResource(R.string.goal_assessment_required))
                    }
                }
            }
        }
    }
}
