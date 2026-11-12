package com.project.speakmate.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun canStartReassessment_returnsFalse_whenLevelNotAssessed() {
        val user = User(
            userId = "123",
            displayName = "Test",
            email = "test@example.com",
            photoUrl = "",
            level = SpeakingLevel.NOT_ASSESSED
        )
        assertFalse(user.canStartReassessment())
    }

    @Test
    fun canStartReassessment_returnsFalse_whenLevelIsB2() {
        val user = User(
            userId = "123",
            displayName = "Test",
            email = "test@example.com",
            photoUrl = "",
            level = SpeakingLevel.B2,
            trainingProgress = TrainingProgress(1, 1, TrainingStage.PATH_COMPLETED)
        )
        assertFalse(user.canStartReassessment())
    }

    @Test
    fun canStartReassessment_returnsTrue_whenPathCompletedAndLevelIsA1OrA2OrB1() {
        val user = User(
            userId = "123",
            displayName = "Test",
            email = "test@example.com",
            photoUrl = "",
            level = SpeakingLevel.A1,
            trainingProgress = TrainingProgress(2, 3, TrainingStage.PATH_COMPLETED)
        )
        assertTrue(user.canStartReassessment())
    }
}
