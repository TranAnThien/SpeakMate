package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentGradingState
import com.project.speakmate.domain.model.AssessmentOutcome
import kotlinx.coroutines.flow.StateFlow

interface AssessmentGradingRepository {
    val gradingState: StateFlow<AssessmentGradingState>
    suspend fun grade(evidence: AssessmentEvidence): AssessmentOutcome
    suspend fun retry(): AssessmentOutcome
    fun clear()
}
