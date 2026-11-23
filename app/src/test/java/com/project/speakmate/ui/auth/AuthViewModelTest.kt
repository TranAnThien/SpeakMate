package com.project.speakmate.ui.auth

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.domain.model.InvalidUserProfileException
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingProgress
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.PlacementTestFixtures
import com.project.speakmate.test.MainDispatcherRule
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    companion object {
        private const val USER_ID = PlacementTestFixtures.USER_ID
        private const val OTHER_USER_ID = "other-user"
        private const val NOW_MILLIS = 2_000L
        private const val VIEW_MODEL_KEY = "auth"
        private const val TOKEN = "sample-token"
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())
    private val auth = mockk<AuthRepository>()
    private val users = mockk<UserRepository>()
    private val signedInUserId = MutableStateFlow<String?>(null)
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW_MILLIS), ZoneOffset.UTC)
    private val store = ViewModelStore()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        every { auth.observeSignedInUserId() } returns signedInUserId
        every { auth.requireCurrentUserId() } answers { checkNotNull(signedInUserId.value) }
        coEvery { auth.signInWithGoogle(any()) } coAnswers {
            signedInUserId.value = USER_ID
            Result.success(USER_ID)
        }
        coEvery { auth.signOut() } coAnswers {
            signedInUserId.value = null
            Result.success(Unit)
        }
        coEvery { users.getUser(any()) } answers { Result.success(profile(firstArg())) }
        coEvery { users.getOrCreateUser(any()) } answers { Result.success(firstArg<User>()) }
        viewModel = AuthViewModel(auth, users)
        store.put(VIEW_MODEL_KEY, viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun retryRestartsASessionObserverAfterUpstreamFailure() = runTest {
        store.clear()
        var subscriptions = 0
        every { auth.observeSignedInUserId() } returns flow {
            subscriptions++
            if (subscriptions == 1) throw IllegalStateException()
            emitAll(signedInUserId)
        }
        viewModel = AuthViewModel(auth, users)
        store.put(VIEW_MODEL_KEY, viewModel)
        advanceUntilIdle()
        viewModel.retryProfile()
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        assertEquals(2, subscriptions)
        assertTrue(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun profileFailureDoesNotPreventObservingAnotherUser() = runTest {
        coEvery { users.getUser(USER_ID) } returns Result.failure(IllegalStateException())
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        assertEquals(GoogleSignInStatus.PROFILE_ERROR, viewModel.uiState.value.status)
        signedInUserId.value = OTHER_USER_ID
        advanceUntilIdle()
        assertEquals(OTHER_USER_ID, viewModel.uiState.value.userId)
        assertTrue(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun signedOutSessionIsReadyForCredentials() = runTest {
        advanceUntilIdle()
        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isCheckingProfile)
        assertFalse(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun cancellingCredentialsKeepsASeparateCancelledState() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.startSignIn())
        viewModel.cancelSignIn()
        assertEquals(GoogleSignInStatus.CANCELLED, viewModel.uiState.value.status)
        coVerify(exactly = 0) { auth.signInWithGoogle(any()) }
    }

    @Test
    fun duplicateCredentialRequestsAreRejected() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.startSignIn())
        assertFalse(viewModel.startSignIn())
        viewModel.handleGoogleSignIn(TOKEN, null, null, null)
        viewModel.handleGoogleSignIn(TOKEN, null, null, null)
        advanceUntilIdle()
        coVerify(exactly = 1) { auth.signInWithGoogle(TOKEN) }
        coVerify { users.getOrCreateUser(match { it.level == SpeakingLevel.NOT_ASSESSED && it.displayName.isEmpty() && it.email.isEmpty() && it.photoUrl.isEmpty() }) }
    }

    @Test
    fun authenticationWaitsForProfileInitializationBeforeReadiness() = runTest {
        val gate = CompletableDeferred<User>()
        coEvery { users.getOrCreateUser(any()) } coAnswers { Result.success(gate.await()) }
        advanceUntilIdle()
        viewModel.startSignIn()
        viewModel.handleGoogleSignIn(TOKEN, null, null, null)
        runCurrent()
        assertFalse(viewModel.uiState.value.isProfileReady)
        assertTrue(viewModel.uiState.value.isCheckingProfile)
        coVerify(exactly = 0) { users.getUser(any()) }
        gate.complete(profile(USER_ID))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun malformedProfileDoesNotNavigateOrSignOutSilently() = runTest {
        coEvery { users.getUser(USER_ID) } returns Result.failure(InvalidUserProfileException("level"))
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        assertEquals(GoogleSignInStatus.PROFILE_ERROR, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isProfileReady)
        coVerify(exactly = 0) { auth.signOut() }
    }

    @Test
    fun missingProfileCanBeRetriedWithoutAnotherGoogleRequest() = runTest {
        coEvery { users.getUser(USER_ID) } returns Result.success(null)
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        assertEquals(GoogleSignInStatus.PROFILE_ERROR, viewModel.uiState.value.status)
        coEvery { users.getUser(USER_ID) } returns Result.success(profile(USER_ID))
        viewModel.retryProfile()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProfileReady)
        coVerify(exactly = 0) { auth.signInWithGoogle(any()) }
    }

    @Test
    fun assessedAccountDoesNotOpenInitialPlacement() = runTest {
        coEvery { users.getUser(USER_ID) } returns Result.success(profile(USER_ID).copy(level = SpeakingLevel.A2, trainingProgress = TrainingProgress(0, 0, TrainingStage.UNIT)))
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun accountSwitchCannotPublishThePreviousProfile() = runTest {
        val gate = CompletableDeferred<Result<User?>>()
        coEvery { users.getUser(USER_ID) } coAnswers { gate.await() }
        signedInUserId.value = USER_ID
        runCurrent()
        signedInUserId.value = OTHER_USER_ID
        advanceUntilIdle()
        gate.complete(Result.success(profile(USER_ID)))
        advanceUntilIdle()
        assertEquals(OTHER_USER_ID, viewModel.uiState.value.userId)
        assertTrue(viewModel.uiState.value.isProfileReady)
    }

    @Test
    fun signOutCleanupSurvivesTheNullUidCallback() = runTest {
        val gate = CompletableDeferred<Unit>()
        var isCleanupFinished = false
        coEvery { auth.signOut() } coAnswers {
            signedInUserId.value = null
            gate.await()
            isCleanupFinished = true
            Result.success(Unit)
        }
        coEvery { users.getUser(USER_ID) } returns Result.success(null)
        signedInUserId.value = USER_ID
        advanceUntilIdle()
        viewModel.signOut()
        runCurrent()
        assertEquals(GoogleSignInStatus.SIGNING_OUT, viewModel.uiState.value.status)
        assertFalse(viewModel.startSignIn())
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(isCleanupFinished)
        assertNull(viewModel.uiState.value.userId)
    }

    private fun profile(userId: String) = User(userId, "Sample", "sample@example.com", "")
}
