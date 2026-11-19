package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.dto.toDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private lateinit var firestoreDataSource: FirestoreUserDataSource
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        firestoreDataSource = mockk()
        repository = UserRepositoryImpl(firestoreDataSource)
    }

    @Test
    fun getOrCreateUserReturnsSuccessWhenDataSourceSucceeds() = runTest {
        val inputUser = User(
            userId = "uid-1",
            displayName = "Quân",
            email = "quan@test.com",
            photoUrl = "",
            level = SpeakingLevel.NOT_ASSESSED
        )
        val savedDto = UserDto(
            userId = "uid-1",
            displayName = "Quân",
            email = "quan@test.com",
            photoUrl = "",
            level = "A1"
        )
        coEvery { firestoreDataSource.getOrCreateUser(any()) } returns savedDto

        val result = repository.getOrCreateUser(inputUser)

        assertTrue(result.isSuccess)
        assertEquals("uid-1", result.getOrNull()!!.userId)
        assertEquals(SpeakingLevel.A1, result.getOrNull()!!.level)
    }

    @Test
    fun getOrCreateUserReturnsFailureWhenDataSourceThrows() = runTest {
        coEvery { firestoreDataSource.getOrCreateUser(any()) } throws RuntimeException("Firestore error")

        val user = User("uid-1", "Test", "t@t.com", "")
        val result = repository.getOrCreateUser(user)

        assertTrue(result.isFailure)
        assertEquals("Firestore error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getUserReturnsUserWhenDocumentExists() = runTest {
        val dto = UserDto(
            userId = "uid-2",
            displayName = "Thiên",
            email = "thien@test.com",
            photoUrl = "https://photo.url/thien.jpg",
            level = "B1"
        )
        coEvery { firestoreDataSource.getUser("uid-2") } returns dto

        val result = repository.getUser("uid-2")

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("uid-2", user!!.userId)
        assertEquals(SpeakingLevel.B1, user.level)
    }

    @Test
    fun getUserReturnsNullWhenDocumentNotExists() = runTest {
        coEvery { firestoreDataSource.getUser("uid-none") } returns null

        val result = repository.getUser("uid-none")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getUserReturnsFailureWhenDataSourceThrows() = runTest {
        coEvery { firestoreDataSource.getUser("uid-err") } throws RuntimeException("Network error")

        val result = repository.getUser("uid-err")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getOrCreateUserPassesCorrectDtoToDataSource() = runTest {
        val user = User(
            userId = "uid-verify",
            displayName = "Verify",
            email = "verify@test.com",
            photoUrl = "https://img.com/v.jpg",
            level = SpeakingLevel.A2
        )
        coEvery { firestoreDataSource.getOrCreateUser(any()) } returns UserDto(
            userId = "uid-verify",
            displayName = "Verify",
            email = "verify@test.com",
            photoUrl = "https://img.com/v.jpg",
            level = "A2"
        )

        repository.getOrCreateUser(user)

        coVerify {
            firestoreDataSource.getOrCreateUser(match { dto ->
                dto.userId == "uid-verify" &&
                dto.displayName == "Verify" &&
                dto.email == "verify@test.com" &&
                dto.level == "A2"
            })
        }
    }

    @Test
    fun observesBothMissingAndValidProfiles() = runTest {
        every { firestoreDataSource.observeUser("uid") } returns flowOf(null, UserDto(userId = "uid", level = "A2"))

        val updates = repository.observeUser("uid").toList()

        assertNull(updates.first())
        assertEquals(SpeakingLevel.A2, updates.last()?.level)
    }

    @Test
    fun returnsInvalidLevelAsLoadFailureWithoutWriting() = runTest {
        coEvery { firestoreDataSource.getUser("uid") } returns UserDto(userId = "uid", level = "UNKNOWN")

        val result = repository.getUser("uid")

        assertTrue(result.exceptionOrNull() is InvalidUserProfileException)
        coVerify(exactly = 0) { firestoreDataSource.getOrCreateUser(any()) }
    }

    @Test
    fun mapsAssessmentFieldsTogetherForTheDataSource() = runTest {
        val criteria = SpeakingCriteriaResult(true, false, true, false, true)
        val progress = TrainingProgress(0, 0, TrainingStage.UNIT)
        val assessedAtMillis = 25_000L
        coEvery { firestoreDataSource.saveAssessedLevel(any(), any(), any(), any()) } returns Unit

        val result = repository.saveAssessedLevel(SpeakingLevel.A2, criteria, assessedAtMillis, progress)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            firestoreDataSource.saveAssessedLevel("A2", criteria.toDto(), Date(assessedAtMillis), progress.toDto())
        }
    }
}
