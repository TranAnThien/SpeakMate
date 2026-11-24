package com.project.speakmate.data.repository

import com.project.speakmate.data.session.AssessmentGradingManager
import com.project.speakmate.domain.model.AssessmentEvidence
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.InsufficientEvidenceReason
import com.project.speakmate.domain.rules.PlacementTestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentGradingRepositoryImplTest {
    @Test
    fun callEvidenceKeepsTaskOwnerDeadlineAndCreatesANewOperationForEachGrade() = runTest {
        val manager = mockk<AssessmentGradingManager>(relaxed = true)
        val outcome = AssessmentOutcome.InsufficientEvidence(InsufficientEvidenceReason.NO_USER_SPEECH)
        val evidence = slot<AssessmentEvidence>()
        coEvery { manager.grade(capture(evidence)) } returns outcome
        val repository = AssessmentGradingRepositoryImpl(manager)
        val task = PlacementTestFixtures.getTasks().first()
        val session = CallSessionState(outcome = CallOutcome.COMPLETED, assessmentAudioId = "audio")
        assertEquals(outcome, repository.gradeCallResult(task, session, "owner", 1L))
        val first = evidence.captured
        assertEquals(task.targetLevel, first.targetLevel)
        assertEquals(task.communicativeGoal, first.communicativeGoal)
        assertEquals(task.taskId, first.taskId)
        assertEquals("owner", first.userId)
        assertEquals(1L, first.expiresAtMillis)
        assertEquals(session.transcript, first.transcript)
        assertEquals(session.failure, first.failure)
        assertEquals(session.hasUserSpeech, first.hasUserSpeech)
        assertEquals(session.outcome, first.callOutcome)
        assertEquals(session.assessmentAudioId, first.assessmentAudioId)
        assertTrue(first.requiredActions.isEmpty())
        repository.gradeCallResult(task, session, "owner", 1L)
        assertTrue(first.operationId != evidence.captured.operationId)
        coVerify(exactly = 2) { manager.grade(any()) }
    }
}
