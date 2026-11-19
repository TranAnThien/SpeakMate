package com.project.speakmate.ui.history

import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.ui.history.state.HistoryResultPresentation

object HistoryResultMapper {
    fun toPresentation(result: AssessmentResult?, type: PracticeType): HistoryResultPresentation = if (type == PracticeType.FREE_PRACTICE) HistoryResultPresentation.COMPLETED else when (result) {
        AssessmentResult.PASS -> HistoryResultPresentation.PASSED
        AssessmentResult.PARTIAL -> HistoryResultPresentation.PARTIAL
        AssessmentResult.FAIL -> HistoryResultPresentation.NOT_PASSED
        null -> HistoryResultPresentation.NOT_EVALUATED
    }
}
