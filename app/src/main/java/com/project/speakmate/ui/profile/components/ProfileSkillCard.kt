package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.R
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileSkillCard(skill: ProfileSkillUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (skill.criterion == null) skill.title else stringResource(when (skill.criterion) {
                AssessmentCriterionType.RANGE -> R.string.placement_range
                AssessmentCriterionType.ACCURACY -> R.string.placement_accuracy
                AssessmentCriterionType.FLUENCY -> R.string.placement_fluency
                AssessmentCriterionType.INTERACTION -> R.string.placement_interaction
                AssessmentCriterionType.COHERENCE, null -> R.string.placement_coherence
            }), style = DesignTypography.Title, color = DesignColor.Text)
            Text(stringResource(if (skill.isPassed) R.string.placement_passed else R.string.profile_skill_improve),
                style = DesignTypography.Detail, color = if (skill.isPassed) DesignColor.Success else DesignColor.ResultWarning)
        }
        Text(skill.description.ifBlank { stringResource(if (skill.isPassed) R.string.placement_criterion_met_feedback else R.string.placement_criterion_unmet_feedback) }, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}
