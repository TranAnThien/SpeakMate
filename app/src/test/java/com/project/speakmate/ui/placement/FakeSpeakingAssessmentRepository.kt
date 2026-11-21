package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import kotlinx.coroutines.CompletableDeferred

class FakeSpeakingAssessmentRepository(var outcome: AssessmentOutcome) : SpeakingAssessmentRepository {
    var evaluations = 0
    var gate: CompletableDeferred<Unit>? = null
    var clearGate: CompletableDeferred<Unit>? = null
    val clearedAudioIds = mutableListOf<String>()
    val evidence = mutableListOf<AssessmentEvidence>()
    var failure: Exception? = null

    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome {
        evaluations++
        this.evidence += evidence
        gate?.await()
        val exception = failure
        if (exception != null) throw exception
        return outcome
    }

    override suspend fun clearAudioEvidence(audioId: String) {
        clearGate?.await()
        clearedAudioIds.add(audioId)
    }
}
