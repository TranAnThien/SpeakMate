package com.project.speakmate.data.dto

import com.project.speakmate.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class UserDtoMappingTest {

    @Test
    fun toDto_mapsAllFieldsCorrectly() {
        val user = User(
            userId = "uid-123",
            displayName = "Quân",
            email = "quan@example.com",
            photoUrl = "https://photo.url/avatar.jpg",
            level = SpeakingLevel.A2,
            latestAssessmentCriteria = SpeakingCriteriaResult(
                rangeMet = true,
                accuracyMet = false,
                fluencyMet = true,
                interactionMet = true,
                coherenceMet = false
            ),
            lastAssessedAt = Date(1700000000000L),
            trainingProgress = TrainingProgress(2, 3, TrainingStage.GOAL_ASSESSMENT),
            savedVocabularies = listOf(
                VocabularyItem("hello", "xin chào", "/həˈloʊ/", "interjection",
                    LearningExample("Hello, how are you?", "Xin chào, bạn khỏe không?"))
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
        assertTrue(dto.latestAssessmentCriteria!!.rangeMet)
        assertFalse(dto.latestAssessmentCriteria!!.accuracyMet)
        assertTrue(dto.latestAssessmentCriteria!!.fluencyMet)
        assertTrue(dto.latestAssessmentCriteria!!.interactionMet)
        assertFalse(dto.latestAssessmentCriteria!!.coherenceMet)

        assertNotNull(dto.trainingProgress)
        assertEquals(2, dto.trainingProgress!!.currentGoalOrder)
        assertEquals(3, dto.trainingProgress!!.currentUnitOrder)
        assertEquals("GOAL_ASSESSMENT", dto.trainingProgress!!.stage)

        assertEquals(1, dto.savedVocabularies.size)
        assertEquals("hello", dto.savedVocabularies[0].word)
        assertEquals("xin chào", dto.savedVocabularies[0].mean)
        assertEquals("/həˈloʊ/", dto.savedVocabularies[0].phonetic)
        assertEquals("interjection", dto.savedVocabularies[0].partOfSpeech)
        assertEquals("Hello, how are you?", dto.savedVocabularies[0].example.sentence)
        assertEquals("Xin chào, bạn khỏe không?", dto.savedVocabularies[0].example.translate)
    }

    @Test
    fun toDto_handlesNullOptionalFields() {
        val user = User(
            userId = "uid-456",
            displayName = "Test",
            email = "test@mail.com",
            photoUrl = ""
        )

        val dto = user.toDto()

        assertEquals("NOT_ASSESSED", dto.level)
        assertNull(dto.latestAssessmentCriteria)
        assertNull(dto.lastAssessedAt)
        assertNull(dto.trainingProgress)
        assertTrue(dto.savedVocabularies.isEmpty())
    }

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val dto = UserDto(
            userId = "uid-789",
            displayName = "Thiên",
            email = "thien@example.com",
            photoUrl = "https://photo.url/thien.jpg",
            level = "B1",
            latestAssessmentCriteria = SpeakingCriteriaResultDto(
                rangeMet = true,
                accuracyMet = true,
                fluencyMet = false,
                interactionMet = true,
                coherenceMet = true
            ),
            lastAssessedAt = Date(1700000000000L),
            trainingProgress = TrainingProgressDto(1, 2, "UNIT"),
            savedVocabularies = listOf(
                VocabularyItemDto("goodbye", "tạm biệt", "/ɡʊdˈbaɪ/", "interjection",
                    LearningExampleDto("Goodbye, see you later!", "Tạm biệt, hẹn gặp lại!"))
            )
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

        assertEquals(1, user.savedVocabularies.size)
        assertEquals("goodbye", user.savedVocabularies[0].word)
        assertEquals("tạm biệt", user.savedVocabularies[0].mean)
    }

    @Test
    fun toDomain_fallsBackToDefaults_whenLevelIsInvalid() {
        val dto = UserDto(
            userId = "uid-invalid",
            displayName = "Test",
            email = "test@mail.com",
            level = "C1_INVALID"
        )

        val user = dto.toDomain()

        assertEquals(SpeakingLevel.NOT_ASSESSED, user.level)
    }

    @Test
    fun toDomain_fallsBackToDefaults_whenTrainingStageIsInvalid() {
        val dto = UserDto(
            userId = "uid-invalid",
            displayName = "Test",
            email = "test@mail.com",
            trainingProgress = TrainingProgressDto(1, 1, "INVALID_STAGE")
        )

        val user = dto.toDomain()

        assertNotNull(user.trainingProgress)
        assertEquals(TrainingStage.UNIT, user.trainingProgress!!.stage)
    }

    @Test
    fun toDomain_handlesNullOptionalFields() {
        val dto = UserDto(
            userId = "uid-empty",
            displayName = "",
            email = "",
            photoUrl = ""
        )

        val user = dto.toDomain()

        assertEquals(SpeakingLevel.NOT_ASSESSED, user.level)
        assertNull(user.latestAssessmentCriteria)
        assertNull(user.lastAssessedAt)
        assertNull(user.trainingProgress)
        assertTrue(user.savedVocabularies.isEmpty())
    }

    @Test
    fun roundTrip_preservesAllData() {
        val original = User(
            userId = "uid-round",
            displayName = "Round Trip",
            email = "round@test.com",
            photoUrl = "https://img.com/round.jpg",
            level = SpeakingLevel.B2,
            latestAssessmentCriteria = SpeakingCriteriaResult(
                rangeMet = true, accuracyMet = true,
                fluencyMet = true, interactionMet = true,
                coherenceMet = false
            ),
            lastAssessedAt = Date(1700000000000L),
            trainingProgress = TrainingProgress(3, 5, TrainingStage.PATH_COMPLETED),
            savedVocabularies = listOf(
                VocabularyItem("test", "kiểm tra", "/tɛst/", "noun",
                    LearningExample("This is a test.", "Đây là bài kiểm tra."))
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
        assertEquals(original.savedVocabularies, restored.savedVocabularies)
    }
}
