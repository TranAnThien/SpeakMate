package com.project.speakmate.ui.components.assessment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R

@Composable
fun assessmentCriterionTitle(criterion: AssessmentCriterionType): String =
    stringResource(
        when (criterion) {
            AssessmentCriterionType.RANGE -> R.string.placement_range
            AssessmentCriterionType.ACCURACY -> R.string.placement_accuracy
            AssessmentCriterionType.FLUENCY -> R.string.placement_fluency
            AssessmentCriterionType.INTERACTION -> R.string.placement_interaction
            AssessmentCriterionType.COHERENCE -> R.string.placement_coherence
        }
    )
