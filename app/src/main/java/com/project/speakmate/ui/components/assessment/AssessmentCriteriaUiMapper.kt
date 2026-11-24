package com.project.speakmate.ui.components.assessment

import com.project.speakmate.domain.model.SpeakingCriteriaResult

object AssessmentCriteriaUiMapper {
    fun toCriteria(criteria: SpeakingCriteriaResult?): List<AssessmentCriterionUiState> {
        if (criteria == null) return emptyList()
        return AssessmentCriterionType.entries.map { type ->
            AssessmentCriterionUiState(
                id = type.name,
                isPassed =
                    when (type) {
                        AssessmentCriterionType.RANGE -> criteria.rangeMet
                        AssessmentCriterionType.ACCURACY -> criteria.accuracyMet
                        AssessmentCriterionType.FLUENCY -> criteria.fluencyMet
                        AssessmentCriterionType.INTERACTION -> criteria.interactionMet
                        AssessmentCriterionType.COHERENCE -> criteria.coherenceMet
                    },
                criterion = type
            )
        }
    }
}
