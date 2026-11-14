package com.project.speakmate.ui.placement

import com.project.speakmate.domain.model.PlacementResult
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.rules.PlacementRules
import com.project.speakmate.domain.rules.PlacementTestFixtures
import com.project.speakmate.ui.placement.state.PlacementPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementRestoreMapperTest {
    private val checkpoint = PlacementTestFixtures.getCheckpoint()
    private val pendingResult = PlacementResult(
        SpeakingLevel.A2,
        SpeakingCriteriaResult(true, true, true, true, false),
        PlacementTestFixtures.CREATED_AT_MILLIS
    )

    @Test
    fun pendingResultWithinLifetimeRestoresResultAndKeepsCheckpoint() {
        val saved = checkpoint.copy(pendingResult = pendingResult)
        val restored = PlacementRestoreMapper.restore(saved, checkpoint.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS - 1)
        assertEquals(PlacementPhase.RESULT, restored.phase)
        assertSame(saved, restored.checkpoint)
        assertEquals(pendingResult, restored.checkpoint?.pendingResult)
        assertFalse(restored.shouldClearCheckpoint)
    }

    @Test
    fun pendingResultAtExpiryRestoresIntroductionAndRequiresCheckpointDeletion() {
        val saved = checkpoint.copy(pendingResult = pendingResult)
        val restored = PlacementRestoreMapper.restore(saved, checkpoint.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS)
        assertEquals(PlacementPhase.INTRODUCTION, restored.phase)
        assertNull(restored.checkpoint)
        assertTrue(restored.shouldClearCheckpoint)
    }

    @Test
    fun unfinishedCheckpointWithinLifetimeRestoresResumePrompt() {
        val restored = PlacementRestoreMapper.restore(checkpoint, checkpoint.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS - 1)
        assertEquals(PlacementPhase.RESUME_PROMPT, restored.phase)
        assertSame(checkpoint, restored.checkpoint)
        assertFalse(restored.shouldClearCheckpoint)
    }

    @Test
    fun unfinishedCheckpointAtExpiryRestoresIntroductionAndRequiresDeletion() {
        val restored = PlacementRestoreMapper.restore(checkpoint, checkpoint.createdAtMillis + PlacementRules.CHECKPOINT_TTL_MILLIS)
        assertEquals(PlacementPhase.INTRODUCTION, restored.phase)
        assertNull(restored.checkpoint)
        assertTrue(restored.shouldClearCheckpoint)
    }

    @Test
    fun missingCheckpointRestoresIntroductionWithoutDeletingAnything() {
        val restored = PlacementRestoreMapper.restore(null, checkpoint.createdAtMillis)
        assertEquals(PlacementPhase.INTRODUCTION, restored.phase)
        assertNull(restored.checkpoint)
        assertFalse(restored.shouldClearCheckpoint)
    }

    @Test
    fun clockRollbackDiscardsPendingResultUsingTheExistingExpiryRule() {
        val restored = PlacementRestoreMapper.restore(checkpoint.copy(pendingResult = pendingResult), checkpoint.createdAtMillis - 1)
        assertEquals(PlacementPhase.INTRODUCTION, restored.phase)
        assertNull(restored.checkpoint)
        assertTrue(restored.shouldClearCheckpoint)
    }
}
