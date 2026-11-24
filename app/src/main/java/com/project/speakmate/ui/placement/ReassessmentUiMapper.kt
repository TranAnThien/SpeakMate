package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.ReassessmentTransition
import com.project.speakmate.ui.placement.state.PromotionResultPresentation
import com.project.speakmate.ui.placement.state.PromotionResultUiState
import com.project.speakmate.ui.placement.state.ReassessmentPhase
import com.project.speakmate.ui.placement.state.ReassessmentUiState

object ReassessmentUiMapper {
    fun toNextStep(state: ReassessmentUiState, transition: ReassessmentTransition.NextStep): ReassessmentUiState =
        state.copy(
            phase = ReassessmentPhase.NEXT_TASK,
            assessment = transition.state,
            result = null
        )

    fun toResult(state: ReassessmentUiState): PromotionResultUiState =
        PromotionResultUiState(
            presentation =
                when (state.result) {
                    is ReassessmentTransition.Promoted -> PromotionResultPresentation.PROMOTED
                    is ReassessmentTransition.NotPromoted -> PromotionResultPresentation.MAINTAINED
                    else -> PromotionResultPresentation.INSUFFICIENT_EVIDENCE
                },
            transition = state.result,
            currentLevel = state.currentLevel,
            nextLevel = state.nextLevel,
            canRetryAssessment = state.result is ReassessmentTransition.RetryTask,
            isSaving = state.isSaving,
            isSaveFailed = state.isSaveFailed,
            isResultSaved = state.isSaved
        )
}
