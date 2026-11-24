package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.text.TitleText

@Composable
fun GoalAssessmentCriteriaContent(
    isIntroduction: Boolean,
    isInsufficientEvidence: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TitleText(
            text =
                stringResource(
                    when {
                        isIntroduction -> R.string.goal_assessment_rules_heading
                        isInsufficientEvidence -> R.string.goal_assessment_unevaluated_heading
                        else -> R.string.goal_assessment_evaluated_heading
                    }
                )
        )
        GoalAssessmentTextCard(
            title =
                stringResource(
                    if (isIntroduction) R.string.goal_assessment_calculation_title
                    else R.string.goal_assessment_criteria_title
                ),
            text =
                stringResource(
                    if (isIntroduction) R.string.goal_assessment_calculation_description
                    else R.string.goal_assessment_criteria_description
                ),
            isHighlighted = false
        )
    }
}
