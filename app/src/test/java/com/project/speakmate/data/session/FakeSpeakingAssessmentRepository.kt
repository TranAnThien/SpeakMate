package com.project.speakmate.data.session

import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException

class FakeSpeakingAssessmentRepository : SpeakingAssessmentRepository {
    var outcome: AssessmentOutcome = PracticeCompletionTestFixtures.evaluated()
    var failure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null
    var cancelledEvaluations = 0
    val evidence = mutableListOf<AssessmentEvidence>()
    val clearedAudioIds = mutableListOf<String>()

    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome {
        this.evidence += evidence
        try {
            gate?.await()
        } catch (cancelled: CancellationException) {
            cancelledEvaluations++
            throw cancelled
        }
        val exception = failure
        if (exception != null) throw exception
        return outcome
    }

    override suspend fun clearAudioEvidence(audioId: String) {
        clearedAudioIds += audioId
    }

}
