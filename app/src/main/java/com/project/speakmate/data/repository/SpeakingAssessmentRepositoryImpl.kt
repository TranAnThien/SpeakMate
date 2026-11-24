package com.project.speakmate.data.repository

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.audio.AssessmentAudioRecorder
import com.project.speakmate.data.remote.ai.GeminiAssessmentClient
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentOwnershipException
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.AssessmentRules
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpeakingAssessmentRepositoryImpl
@Inject
constructor(
    private val audioFiles: AssessmentAudioFileProvider,
    private val client: GeminiAssessmentClient,
    private val authRepository: AuthRepository,
    private val clock: Clock
) : SpeakingAssessmentRepository {
    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome =
        withContext(Dispatchers.IO) {
            val uid = requireOwner(evidence)
            val outcome =
                try {
                    evaluateEvidence(evidence)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (stale: AssessmentOwnershipException) {
                    throw stale
                } catch (_: Exception) {
                    AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
                }
            if (requireOwner(evidence) != uid) throw AssessmentOwnershipException()
            outcome
        }

    override suspend fun clearAudioEvidence(audioId: String): Unit =
        withContext(Dispatchers.IO) { audioFiles.deleteAudio(audioId) }

    private suspend fun evaluateEvidence(evidence: AssessmentEvidence): AssessmentOutcome {
        val problem = AssessmentRules.findEvidenceProblem(evidence)
        if (problem != null) return AssessmentOutcome.InsufficientEvidence(problem)
        if (isExpired(evidence)) return unavailable()
        val audioId = evidence.assessmentAudioId ?: return unavailable()
        val file =
            audioFiles.getAudioFile(audioId)
                ?: return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        val wav =
            try {
                AssessmentAudioRecorder.getAudioBytes(file)
            } catch (_: IOException) {
                return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
            } ?: return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        val evaluation = client.evaluate(evidence, wav)
        if (isExpired(evidence)) return unavailable()
        return AssessmentRules.assess(evidence, evaluation)
    }

    private fun requireOwner(evidence: AssessmentEvidence): String {
        val uid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                throw AssessmentOwnershipException()
            }
        if (evidence.userId != null && evidence.userId != uid) throw AssessmentOwnershipException()
        return uid
    }

    private fun isExpired(evidence: AssessmentEvidence): Boolean =
        evidence.expiresAtMillis?.let { clock.millis() >= it } == true

    private fun unavailable() = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
}
