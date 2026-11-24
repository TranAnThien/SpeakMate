package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.ui.components.assessment.AssessmentCriterionType
import com.project.speakmate.ui.training.state.UnitResultCriterionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.utils.extension.defaultEmpty

object TrainingResultMapper {
    fun toCriteria(outcome: AssessmentOutcome.Evaluated?): List<UnitResultCriterionUiState> =
        AssessmentCriterionType.entries.map { type ->
            val passed =
                when (type) {
                    AssessmentCriterionType.RANGE -> outcome?.criteria?.rangeMet
                    AssessmentCriterionType.ACCURACY -> outcome?.criteria?.accuracyMet
                    AssessmentCriterionType.FLUENCY -> outcome?.criteria?.fluencyMet
                    AssessmentCriterionType.INTERACTION -> outcome?.criteria?.interactionMet
                    AssessmentCriterionType.COHERENCE -> outcome?.criteria?.coherenceMet
                }
            val feedback =
                when (type) {
                    AssessmentCriterionType.RANGE -> outcome?.evaluation?.range?.feedback
                    AssessmentCriterionType.ACCURACY -> outcome?.evaluation?.accuracy?.feedback
                    AssessmentCriterionType.FLUENCY -> outcome?.evaluation?.fluency?.feedback
                    AssessmentCriterionType.INTERACTION -> outcome?.evaluation?.interaction?.feedback
                    AssessmentCriterionType.COHERENCE -> outcome?.evaluation?.coherence?.feedback
                }
            UnitResultCriterionUiState(
                id = type.name,
                description = feedback.defaultEmpty(),
                evaluation = evaluation(passed),
                criterion = type
            )
        }

    fun evaluation(isMet: Boolean?): UnitResultEvaluation =
        when (isMet) {
            true -> UnitResultEvaluation.PASSED
            false -> UnitResultEvaluation.NOT_PASSED
            null -> UnitResultEvaluation.NOT_EVALUATED
        }
}
