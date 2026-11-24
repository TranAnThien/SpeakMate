package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.AssessmentCriterionType
import com.project.speakmate.ui.components.assessment.AssessmentCriterionUiState
import com.project.speakmate.ui.components.assessment.assessmentCriterionTitle
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileSkillCard(skill: AssessmentCriterionUiState, modifier: Modifier = Modifier) {
    val title = assessmentCriterionTitle(skill.criterion)
    val description =
        when (skill.criterion) {
            AssessmentCriterionType.RANGE -> stringResource(R.string.profile_criterion_range_description)
            AssessmentCriterionType.ACCURACY -> stringResource(R.string.profile_criterion_accuracy_description)
            AssessmentCriterionType.FLUENCY -> stringResource(R.string.profile_criterion_fluency_description)
            AssessmentCriterionType.INTERACTION -> stringResource(R.string.profile_criterion_interaction_description)
            AssessmentCriterionType.COHERENCE -> stringResource(R.string.profile_criterion_coherence_description)
        }
    SpeakMateCard(modifier = modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TitleText(text = title)
                if (description.isNotBlank()) DetailText(text = description)
            }
            CaptionText(
                text =
                    stringResource(
                        if (skill.isPassed) R.string.placement_passed else R.string.profile_criterion_not_met
                    ),
                style = DesignTypography.Badge,
                color = if (skill.isPassed) DesignColor.OnSuccessContainer else DesignColor.OnErrorContainer,
                modifier =
                    Modifier
                        .widthIn(max = 112.dp)
                        .background(
                            if (skill.isPassed) DesignColor.SuccessContainer else DesignColor.ErrorSurface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
