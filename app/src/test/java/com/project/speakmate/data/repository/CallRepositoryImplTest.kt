package com.project.speakmate.data.repository

import com.project.speakmate.data.session.CallSessionManager
import com.project.speakmate.data.session.FakeCallSessionMedia
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.Scenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallRepositoryImplTest {
    private val scenario = Scenario("cafe", "Cafe", "Order a drink", "Act as a barista")

    @Test
    fun placementPromptContainsGoalAndAssistanceRestriction() = runTest {
        val media = FakeCallSessionMedia()
        val manager = CallSessionManager({ media }, StandardTestDispatcher(testScheduler)) { testScheduler.currentTime }
        try {
            manager.setForeground(true)
            CallRepositoryImpl(manager).startCall(scenario, CallActivityType.INITIAL_PLACEMENT, "Ask for a medium latte")
            runCurrent()
            val prompt = checkNotNull(media.connectedScenario).promptTemplate
            assertTrue(prompt.contains(scenario.promptTemplate))
            assertTrue(prompt.contains("Ask for a medium latte"))
            assertTrue(prompt.contains("Do not correct the learner"))
            assertFalse(prompt.contains("Activity: Free Practice."))
        } finally {
            manager.close()
            runCurrent()
        }
    }

    @Test
    fun freePracticeWithEmptyGoalKeepsItsPromptWithoutAssessmentRestriction() = runTest {
        val media = FakeCallSessionMedia()
        val manager = CallSessionManager({ media }, StandardTestDispatcher(testScheduler)) { testScheduler.currentTime }
        try {
            manager.setForeground(true)
            CallRepositoryImpl(manager).startCall(scenario, CallActivityType.FREE_PRACTICE, "")
            runCurrent()
            val prompt = checkNotNull(media.connectedScenario).promptTemplate
            assertTrue(prompt.contains(scenario.promptTemplate))
            assertTrue(prompt.contains("Activity: Free Practice."))
            assertFalse(prompt.contains("Do not correct the learner"))
        } finally {
            manager.close()
            runCurrent()
        }
    }
}
