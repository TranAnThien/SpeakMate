package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.model.VocabularyItem
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDtoMappingTest {

    @Test
    fun toDtoMapsAllFieldsCorrectly() {
        val user =
            User(
                userId = "uid-123",
                displayName = "Quân",
                email = "quan@example.com",
                photoUrl = "https://photo.url/avatar.jpg",
                level = SpeakingLevel.A2,
                latestAssessmentCriteria =
                    SpeakingCriteriaResult(
                        rangeMet = true,
                        accuracyMet = false,
                        fluencyMet = true,
                        interactionMet = true,
                        coherenceMet = false
                    ),
                lastAssessedAt = Date(1700000000000L),
                trainingProgress =
                    TrainingProgress(currentGoalOrder = 2, currentUnitOrder = 3, stage = TrainingStage.GOAL_ASSESSMENT),
                savedVocabularies =
                    listOf(
                        VocabularyItem(
                            word = "hello",
                            mean = "xin chào",
                            phonetic = "/həˈloʊ/",
                            partOfSpeech = "interjection",
                            example = LearningExample("Hello, how are you?", "Xin chào, bạn khỏe không?")
                        )
                    )
            )

        val dto = user.toDto()

        assertEquals("uid-123", dto.userId)
        assertEquals("Quân", dto.displayName)
        assertEquals("quan@example.com", dto.email)
        assertEquals("https://photo.url/avatar.jpg", dto.photoUrl)
        assertEquals("A2", dto.level)
        assertEquals(Date(1700000000000L), dto.lastAssessedAt)

        assertNotNull(dto.latestAssessmentCriteria)
        assertEquals(true, dto.latestAssessmentCriteria?.rangeMet)
        assertEquals(false, dto.latestAssessmentCriteria?.accuracyMet)
        assertEquals(true, dto.latestAssessmentCriteria?.fluencyMet)
        assertEquals(true, dto.latestAssessmentCriteria?.interactionMet)
        assertEquals(false, dto.latestAssessmentCriteria?.coherenceMet)

        assertNotNull(dto.trainingProgress)
        assertEquals(2, dto.trainingProgress!!.currentGoalOrder)
        assertEquals(3, dto.trainingProgress!!.currentUnitOrder)
        assertEquals("GOAL_ASSESSMENT", dto.trainingProgress!!.stage)
    }

    @Test
    fun toDtoHandlesNullOptionalFields() {
        val user = User(userId = "uid-456", displayName = "Test", email = "test@mail.com", photoUrl = "")

        val dto = user.toDto()

        assertEquals("NOT_ASSESSED", dto.level)
        assertNull(dto.latestAssessmentCriteria)
        assertNull(dto.lastAssessedAt)
        assertNull(dto.trainingProgress)
    }

    @Test
    fun toDomainMapsAllFieldsCorrectly() {
        val dto =
            UserDto(
                userId = "uid-789",
                displayName = "Thiên",
                email = "thien@example.com",
                photoUrl = "https://photo.url/thien.jpg",
                level = "B1",
                latestAssessmentCriteria =
                    SpeakingCriteriaResultDto(
                        rangeMet = true,
                        accuracyMet = true,
                        fluencyMet = false,
                        interactionMet = true,
                        coherenceMet = true
                    ),
                lastAssessedAt = Date(1700000000000L),
                trainingProgress = TrainingProgressDto(currentGoalOrder = 1, currentUnitOrder = 2, stage = "UNIT")
            )

        val user = dto.toDomain()

        assertEquals("uid-789", user.userId)
        assertEquals("Thiên", user.displayName)
        assertEquals("thien@example.com", user.email)
        assertEquals("https://photo.url/thien.jpg", user.photoUrl)
        assertEquals(SpeakingLevel.B1, user.level)
        assertEquals(Date(1700000000000L), user.lastAssessedAt)

        assertNotNull(user.latestAssessmentCriteria)
        assertTrue(user.latestAssessmentCriteria!!.rangeMet)
        assertTrue(user.latestAssessmentCriteria!!.accuracyMet)
        assertFalse(user.latestAssessmentCriteria!!.fluencyMet)
        assertTrue(user.latestAssessmentCriteria!!.interactionMet)
        assertTrue(user.latestAssessmentCriteria!!.coherenceMet)

        assertNotNull(user.trainingProgress)
        assertEquals(1, user.trainingProgress!!.currentGoalOrder)
        assertEquals(2, user.trainingProgress!!.currentUnitOrder)
        assertEquals(TrainingStage.UNIT, user.trainingProgress!!.stage)

        assertTrue(user.savedVocabularies.isEmpty())
    }

    @Test
    fun rejectsAnInvalidProfileLevel() {
        val dto = UserDto(userId = "uid-invalid", displayName = "Test", email = "test@mail.com", level = "C1_INVALID")

        assertThrows(InvalidUserProfileException::class.java) { dto.toDomain() }
    }

    @Test
    fun legacyInProgressStageMapsToUnitWithoutChangingStoredValue() {
        val progress = TrainingProgressDto(currentGoalOrder = 1, currentUnitOrder = 1, stage = "IN_PROGRESS")
        val dto = UserDto(trainingProgress = progress)

        val restored = requireNotNull(dto.toDomain().trainingProgress)

        assertEquals(TrainingStage.UNIT, restored.stage)
        assertEquals(progress.currentGoalOrder, restored.currentGoalOrder)
        assertEquals(progress.currentUnitOrder, restored.currentUnitOrder)
        assertEquals("IN_PROGRESS", progress.stage)
    }

    @Test
    fun legacyInProgressStageStillRejectsMissingProgressCoordinates() {
        val dto = UserDto(
            trainingProgress = TrainingProgressDto(currentGoalOrder = 1, currentUnitOrder = null, stage = "IN_PROGRESS")
        )

        assertThrows(InvalidUserProfileException::class.java) { dto.toDomain() }
    }

    @Test
    fun rejectsAnInvalidTrainingStage() {
        val dto =
            UserDto(
                userId = "uid-invalid",
                displayName = "Test",
                email = "test@mail.com",
                trainingProgress =
                    TrainingProgressDto(currentGoalOrder = 1, currentUnitOrder = 1, stage = "INVALID_STAGE")
            )

        assertThrows(InvalidUserProfileException::class.java) { dto.toDomain() }
    }

    @Test
    fun rejectsCriteriaWithMissingRequiredField() {
        val dto =
            UserDto(
                userId = "uid-invalid",
                displayName = "Test",
                email = "test@mail.com",
                latestAssessmentCriteria =
                    SpeakingCriteriaResultDto(
                        rangeMet = true,
                        accuracyMet = true,
                        fluencyMet = null,
                        interactionMet = true,
                        coherenceMet = true
                    )
            )

        assertThrows(InvalidUserProfileException::class.java) { dto.toDomain() }
    }

    @Test
    fun rejectsTrainingProgressWithMissingRequiredField() {
        val dto =
            UserDto(
                userId = "uid-invalid",
                displayName = "Test",
                email = "test@mail.com",
                trainingProgress = TrainingProgressDto(currentGoalOrder = 1, currentUnitOrder = null, stage = "UNIT")
            )

        assertThrows(InvalidUserProfileException::class.java) { dto.toDomain() }
    }

    @Test
    fun toDomainHandlesNullOptionalFields() {
        val dto = UserDto(userId = "uid-empty", displayName = "", email = "", photoUrl = "")

        val user = dto.toDomain()

        assertEquals(SpeakingLevel.NOT_ASSESSED, user.level)
        assertNull(user.latestAssessmentCriteria)
        assertNull(user.lastAssessedAt)
        assertNull(user.trainingProgress)
        assertTrue(user.savedVocabularies.isEmpty())
    }

    @Test
    fun roundTripPreservesProfileWhileVocabularyHasItsOwnRepository() {
        val original =
            User(
                userId = "uid-round",
                displayName = "Round Trip",
                email = "round@test.com",
                photoUrl = "https://img.com/round.jpg",
                level = SpeakingLevel.B2,
                latestAssessmentCriteria =
                    SpeakingCriteriaResult(
                        rangeMet = true,
                        accuracyMet = true,
                        fluencyMet = true,
                        interactionMet = true,
                        coherenceMet = false
                    ),
                lastAssessedAt = Date(1700000000000L),
                trainingProgress =
                    TrainingProgress(currentGoalOrder = 3, currentUnitOrder = 5, stage = TrainingStage.PATH_COMPLETED),
                savedVocabularies =
                    listOf(
                        VocabularyItem(
                            word = "test",
                            mean = "kiểm tra",
                            phonetic = "/tɛst/",
                            partOfSpeech = "noun",
                            example = LearningExample("This is a test.", "Đây là bài kiểm tra.")
                        )
                    )
            )

        val restored = original.toDto().toDomain()

        assertEquals(original.userId, restored.userId)
        assertEquals(original.displayName, restored.displayName)
        assertEquals(original.email, restored.email)
        assertEquals(original.photoUrl, restored.photoUrl)
        assertEquals(original.level, restored.level)
        assertEquals(original.lastAssessedAt, restored.lastAssessedAt)
        assertEquals(original.latestAssessmentCriteria, restored.latestAssessmentCriteria)
        assertEquals(original.trainingProgress, restored.trainingProgress)
        assertTrue(restored.savedVocabularies.isEmpty())
    }
}
