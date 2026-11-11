package com.project.speakmate.data.repository

import com.project.speakmate.data.audio.AssessmentAudioFileProvider
import com.project.speakmate.data.remote.ai.GeminiAssessmentClient
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.repository.SpeakingAssessmentRepository
import com.project.speakmate.domain.rules.AssessmentRules
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class SpeakingAssessmentRepositoryImpl @Inject constructor(
    private val audioFiles: AssessmentAudioFileProvider,
    private val client: GeminiAssessmentClient
) : SpeakingAssessmentRepository {
    override suspend fun evaluate(evidence: AssessmentEvidence): AssessmentOutcome = withContext(Dispatchers.IO) {
        var cancellation: CancellationException? = null
        try {
            try {
                evaluateEvidence(evidence)
            } catch (cancelled: CancellationException) {
                cancellation = cancelled
                throw cancelled
            } finally {
                try {
                    withContext(NonCancellable) {
                        evidence.assessmentAudioId?.let(audioFiles::deleteAudio)
                    }
                } catch (cleanupFailure: Exception) {
                    val cancelled = cancellation
                    if (cancelled != null) throw cancelled
                    throw cleanupFailure
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.EVALUATION_FAILED)
        }
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
            file.readBytes()
        } catch (_: IOException) {
            return AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.AUDIO_UNAVAILABLE)
        }
        val evaluation = client.evaluate(evidence, wav)
        return AssessmentRules.assess(evidence, evaluation)
    }
}
