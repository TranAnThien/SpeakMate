package com.project.speakmate.ui.profile

import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.FakeUserRepository
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.profile.state.ProfileEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        assertEquals(SpeakingLevel.NOT_ASSESSED, viewModel.uiState.value.level)

        users.setUser(user().copy(level = SpeakingLevel.A2))
        advanceUntilIdle()

        assertEquals(SpeakingLevel.A2, viewModel.uiState.value.level)
        assertFalse(viewModel.uiState.value.isProfileLoadFailed)
    }

    @Test
    fun observationFailureKeepsTheSessionAndAllowsRetry() = runTest {
        users.shouldFailObservation = true
        val viewModel = ProfileViewModel(auth, users)
        assertTrue(viewModel.uiState.value.isProfileLoadFailed)
        assertEquals("user-1", auth.signedInUserId.value)

        users.shouldFailObservation = false
        viewModel.retryProfile()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProfileLoadFailed)
        assertEquals("student@example.com", viewModel.uiState.value.email)
        assertEquals(2, users.observationStarts)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun switchingAccountsCancelsThePreviousProfileListener() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        users.setUser(user().copy(userId = "user-2", displayName = "Second student", level = SpeakingLevel.B1))

        auth.signedInUserId.value = "user-2"
        advanceUntilIdle()
        users.setUser(user().copy(level = SpeakingLevel.B2))
        advanceUntilIdle()

        assertEquals("Second student", viewModel.uiState.value.displayName)
        assertEquals(SpeakingLevel.B1, viewModel.uiState.value.level)
        assertEquals(1, users.activeObservers)
    }

    @Test
    fun repeatedRetryKeepsOneProfileListener() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        viewModel.retryProfile()
        viewModel.retryProfile()
        advanceUntilIdle()

        assertEquals(1, users.activeObservers)
        assertEquals("student@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun signedOutStateClearsTheProfile() = runTest {
        auth.signedInUserId.value = null
        val viewModel = ProfileViewModel(auth, users)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.email.isEmpty())
        assertFalse(viewModel.uiState.value.isProfileLoadFailed)
    }

    @Test
    fun signOutEmitsTheEffectAfterTheRepositorySignsOut() = runTest {
        val viewModel = ProfileViewModel(auth, users)
        val effects = mutableListOf<ProfileEffect>()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect {
                assertNull(auth.signedInUserId.value)
                effects.add(it)
            }
        }
        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(listOf(ProfileEffect.SignedOut), effects)
        observer.cancel()
        assertTrue(viewModel.uiState.value.email.isEmpty())
    }

    @Test
    fun profileMapsEveryCriterionAsMeaningInsteadOfDisplayText() = runTest {
        users.setUser(user().copy(level = SpeakingLevel.B1, latestAssessmentCriteria = SpeakingCriteriaResult(true, false, true, false, true)))
        val viewModel = ProfileViewModel(auth, users)
        advanceUntilIdle()
        assertEquals(SpeakingLevel.B1, viewModel.uiState.value.level)
        assertEquals(5, viewModel.uiState.value.skills.size)
        assertEquals(listOf(true, false, true, false, true), viewModel.uiState.value.skills.map { it.isPassed })
        assertTrue(viewModel.uiState.value.skills.all { it.criterion != null && it.title.isEmpty() })
    }

    private fun user() = User("user-1", "Student", "student@example.com", "")
}
