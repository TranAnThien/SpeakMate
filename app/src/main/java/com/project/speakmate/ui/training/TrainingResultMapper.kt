package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.ui.training.state.UnitResultCriterionType
import com.project.speakmate.ui.training.state.UnitResultCriterionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.utils.extension.defaultEmpty

object TrainingResultMapper {
    fun toCriteria(outcome: AssessmentOutcome.Evaluated?): List<UnitResultCriterionUiState> =
        UnitResultCriterionType.entries.map { type ->
            val passed = when (type) {
                UnitResultCriterionType.RANGE -> outcome?.criteria?.rangeMet
                UnitResultCriterionType.ACCURACY -> outcome?.criteria?.accuracyMet
                UnitResultCriterionType.FLUENCY -> outcome?.criteria?.fluencyMet
                UnitResultCriterionType.INTERACTION -> outcome?.criteria?.interactionMet
                UnitResultCriterionType.COHERENCE -> outcome?.criteria?.coherenceMet
            }
            val feedback = when (type) {
                UnitResultCriterionType.RANGE -> outcome?.evaluation?.range?.feedback
                UnitResultCriterionType.ACCURACY -> outcome?.evaluation?.accuracy?.feedback
                UnitResultCriterionType.FLUENCY -> outcome?.evaluation?.fluency?.feedback
                UnitResultCriterionType.INTERACTION -> outcome?.evaluation?.interaction?.feedback
                UnitResultCriterionType.COHERENCE -> outcome?.evaluation?.coherence?.feedback
            }
            UnitResultCriterionUiState(type.name, description = feedback.defaultEmpty(), evaluation = toEvaluation(passed), criterion = type)
        }

    private fun toEvaluation(isMet: Boolean?): UnitResultEvaluation = when (isMet) {
        true -> UnitResultEvaluation.PASSED
        false -> UnitResultEvaluation.NOT_PASSED
        null -> UnitResultEvaluation.NOT_EVALUATED
    }
}
