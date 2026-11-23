package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.ui.components.assessmentCriterionTitle
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileSkillCard(skill: ProfileSkillUiState, modifier: Modifier = Modifier) {
    val title = if (skill.criterion == null) skill.title else assessmentCriterionTitle(checkNotNull(skill.criterion))
    val description = when (skill.criterion) {
        AssessmentCriterionType.RANGE -> stringResource(R.string.profile_criterion_range_description)
        AssessmentCriterionType.ACCURACY -> stringResource(R.string.profile_criterion_accuracy_description)
        AssessmentCriterionType.FLUENCY -> stringResource(R.string.profile_criterion_fluency_description)
        AssessmentCriterionType.INTERACTION -> stringResource(R.string.profile_criterion_interaction_description)
        AssessmentCriterionType.COHERENCE -> stringResource(R.string.profile_criterion_coherence_description)
        null -> skill.description
    }
    Row(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DesignTypography.Title, color = DesignColor.Text)
            if (description.isNotBlank()) Text(description, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
        Text(stringResource(if (skill.isPassed) R.string.placement_passed else R.string.profile_criterion_not_met),
            style = DesignTypography.Badge, color = if (skill.isPassed) DesignColor.OnSuccessContainer else DesignColor.OnErrorContainer,
            modifier = Modifier.widthIn(max = 112.dp).background(if (skill.isPassed) DesignColor.SuccessContainer else DesignColor.ErrorSurface,
                RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
