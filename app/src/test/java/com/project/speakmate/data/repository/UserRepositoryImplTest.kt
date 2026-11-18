package com.project.speakmate.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.project.speakmate.data.dto.UserDto
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private lateinit var firestoreDataSource: FirestoreUserDataSource
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        firestoreDataSource = mockk()
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        repository = UserRepositoryImpl(firestoreDataSource, firestore, auth)
    }

    @Test
    fun getOrCreateUser_returnsSuccess_whenDataSourceSucceeds() = runTest {
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
    fun getOrCreateUser_returnsFailure_whenDataSourceThrows() = runTest {
        coEvery { firestoreDataSource.getOrCreateUser(any()) } throws RuntimeException("Firestore error")

        val user = User("uid-1", "Test", "t@t.com", "")
        val result = repository.getOrCreateUser(user)

        assertTrue(result.isFailure)
        assertEquals("Firestore error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getUser_returnsUser_whenDocumentExists() = runTest {
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
    fun getUser_returnsNull_whenDocumentNotExists() = runTest {
        coEvery { firestoreDataSource.getUser("uid-none") } returns null

        val result = repository.getUser("uid-none")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getUser_returnsFailure_whenDataSourceThrows() = runTest {
        coEvery { firestoreDataSource.getUser("uid-err") } throws RuntimeException("Network error")

        val result = repository.getUser("uid-err")

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getOrCreateUser_passesCorrectDtoToDataSource() = runTest {
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
    fun updateUserLevelAndProgress_fails_whenUserNotLoggedIn() = runTest {
        every { auth.currentUser } returns null

        val result = repository.updateUserLevelAndProgress(
            SpeakingLevel.B1,
            SpeakingCriteriaResult(true, true, true, true, true),
            TrainingProgress(0, 0, TrainingStage.UNIT)
        )

        assertTrue(result.isFailure)
        assertEquals("User not logged in", result.exceptionOrNull()?.message)
    }
}
