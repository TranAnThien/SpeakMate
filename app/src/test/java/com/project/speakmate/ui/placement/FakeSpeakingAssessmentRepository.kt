package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import kotlinx.coroutines.CompletableDeferred

class FakeSpeakingAssessmentRepository(var outcome: AssessmentOutcome) : SpeakingAssessmentRepository {
    var evaluations = 0
    var gate: CompletableDeferred<Unit>? = null
    val clearedAudioIds = mutableListOf<String>()

    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome {
        evaluations++
        gate?.await()
        return outcome
    }

    override suspend fun clearAudioEvidence(audioId: String) {
        clearedAudioIds.add(audioId)
    }
}
