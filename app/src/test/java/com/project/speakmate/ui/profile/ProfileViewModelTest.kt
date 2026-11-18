package com.project.speakmate.ui.profile

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository(user())

    @Test
    fun observesTheInitialProfileAndLaterLevelChanges() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        assertEquals(SpeakingLevel.NOT_ASSESSED, viewModel.userProfile.value?.level)

        users.setUser(user().copy(level = SpeakingLevel.A2))
        advanceUntilIdle()

        assertEquals(SpeakingLevel.A2, viewModel.userProfile.value?.level)
        assertFalse(viewModel.isProfileLoadFailed.value)
    }

    @Test
    fun observationFailureKeepsTheSessionAndAllowsRetry() = runTest {
        users.shouldFailObservation = true
        val viewModel = ProfileViewModel(auth, users)
        assertTrue(viewModel.isProfileLoadFailed.value)
        assertEquals("user-1", auth.signedInUserId.value)

        users.shouldFailObservation = false
        viewModel.retryProfile()
        advanceUntilIdle()

        assertFalse(viewModel.isProfileLoadFailed.value)
        assertEquals("user-1", viewModel.userProfile.value?.userId)
        assertEquals(2, users.observationStarts)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun switchingAccountsCancelsThePreviousProfileListener() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        users.setUser(user().copy(userId = "user-2", level = SpeakingLevel.B1))

        auth.signedInUserId.value = "user-2"
        advanceUntilIdle()
        users.setUser(user().copy(level = SpeakingLevel.B2))
        advanceUntilIdle()

        assertEquals("user-2", viewModel.userProfile.value?.userId)
        assertEquals(SpeakingLevel.B1, viewModel.userProfile.value?.level)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun repeatedRetryKeepsOneProfileListener() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        viewModel.retryProfile()
        viewModel.retryProfile()
        advanceUntilIdle()

        assertEquals(1, users.activeObservers)
        assertEquals("user-1", viewModel.userProfile.value?.userId)
    }

    @Test
    fun signedOutStateClearsTheProfile() = runTest {
        auth.signedInUserId.value = null
        val viewModel = ProfileViewModel(auth, users)
        advanceUntilIdle()

        assertNull(viewModel.userProfile.value)
        assertFalse(viewModel.isProfileLoadFailed.value)
    }

    @Test
    fun signOutCallsTheRepositoryBeforeTheCallback() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        var hasCalledBack = false

        viewModel.signOut {
            assertNull(auth.signedInUserId.value)
            hasCalledBack = true
        }
        advanceUntilIdle()

        assertTrue(hasCalledBack)
        assertNull(viewModel.userProfile.value)
    }

    private fun user() = User("user-1", "Student", "student@example.com", "")
}
