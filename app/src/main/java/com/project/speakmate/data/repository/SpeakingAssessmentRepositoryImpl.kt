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
import com.project.speakmate.domain.rules.PracticeCompletionRules
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class SpeakingAssessmentRepositoryImpl @Inject constructor(
    private val audioFiles: AssessmentAudioFileProvider,
    private val client: GeminiAssessmentClient,
    private val authRepository: AuthRepository,
    private val clock: Clock
) : SpeakingAssessmentRepository {
    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome = withContext(Dispatchers.IO) {
        val uid = authRepository.requireCurrentUserId()
        if (evidence.userId != null && evidence.userId != uid) throw AssessmentOwnershipException()
        val audioId = evidence.assessmentAudioId
        val outcome = try {
            val problem = AssessmentRules.findEvidenceProblem(evidence)
            if (problem != null) AssessmentOutcome.InsufficientEvidence(problem)
            else {
                when {
                    audioId == null || audioFiles.getAudioFile(audioId) == null ->
                        AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
                    evidence.expiresAtMillis?.let { clock.millis() >= it } == true ->
                        AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
                    else -> evaluateEvidence(evidence)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
        if (authRepository.requireCurrentUserId() != uid) throw AssessmentOwnershipException()
        if (outcome !is AssessmentOutcome.InsufficientEvidence || !PracticeCompletionRules.canRetryGrading(outcome.reason)) {
            deleteEvidenceAudio(evidence)
        }
        outcome
    }

    override suspend fun clearAudioEvidence(audioId: String): Unit = withContext(Dispatchers.IO) {
        audioFiles.deleteAudio(audioId)
    }

    private suspend fun evaluateEvidence(evidence: AssessmentEvidence): AssessmentOutcome {
        val problem = AssessmentRules.findEvidenceProblem(evidence)
        if (problem != null) return AssessmentOutcome.InsufficientEvidence(problem)
        val audioId = evidence.assessmentAudioId ?: return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        val file = audioFiles.getAudioFile(audioId)
            ?: return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        val wav = try {
            AssessmentAudioRecorder.getAudioBytes(file)
        } catch (_: IOException) {
            return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        } ?: return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        val userId = authRepository.requireCurrentUserId()
        if (evidence.userId != null && evidence.userId != userId) throw AssessmentOwnershipException()
        val evaluation = client.evaluate(evidence, wav)
        if (authRepository.requireCurrentUserId() != userId) throw AssessmentOwnershipException()
        if (evidence.expiresAtMillis?.let { clock.millis() >= it } == true) {
            return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        }
        return AssessmentRules.assess(evidence, evaluation)
    }

    private suspend fun deleteEvidenceAudio(evidence: AssessmentEvidence): Boolean = withContext(NonCancellable) {
        try {
            evidence.assessmentAudioId?.let { audioFiles.deleteAudio(it) }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}
