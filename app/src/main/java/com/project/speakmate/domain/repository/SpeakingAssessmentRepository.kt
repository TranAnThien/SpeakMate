package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome

interface SpeakingAssessmentRepository {
    suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome
    suspend fun clearAudioEvidence(audioId: String)
}
