package com.project.speakmate.ui.auth

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: AuthViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        every { authRepository.getSignedInUserId() } returns flowOf(null)
        viewModel = AuthViewModel(authRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isNormal() {
        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
    }

    @Test
    fun onGoogleSignInCancelled_setsStatusNormal() {
        viewModel.onGoogleSignInCancelled()

        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
    }

    @Test
    fun handleGoogleSignIn_success_emitsSignInSuccess() = runTest {
        coEvery { authRepository.signInWithGoogle("valid-token") } returns Result.success("uid-1")
        coEvery { userRepository.getOrCreateUser(any()) } returns Result.success(
            User("uid-1", "Test", "test@mail.com", "", SpeakingLevel.NOT_ASSESSED)
        )

        val events = mutableListOf<Unit>()
        val job = launch(testDispatcher) {
            viewModel.signInSuccess.collect { events.add(it) }
        }

        viewModel.handleGoogleSignIn("valid-token", "Test", "test@mail.com", null)

        assertEquals(1, events.size)
        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
        job.cancel()
    }

    @Test
    fun handleGoogleSignIn_returnsToNormalAfterSuccessfulAuth() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success("uid-1")
        coEvery { userRepository.getOrCreateUser(any()) } returns Result.success(
            User("uid-1", "Test", "t@t.com", "")
        )

        viewModel.handleGoogleSignIn("token", "Test", "t@t.com", null)

        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
        coVerify { authRepository.signInWithGoogle("token") }
    }

    @Test
    fun handleGoogleSignIn_firebaseAuthFails_setsError() = runTest {
        coEvery { authRepository.signInWithGoogle("bad-token") } returns
                Result.failure(RuntimeException("Firebase Auth failed"))

        viewModel.handleGoogleSignIn("bad-token", "Test", "t@t.com", null)

        assertEquals(GoogleSignInStatus.ERROR, viewModel.uiState.value.status)
    }

    @Test
    fun handleGoogleSignIn_profileCreationFails_rollsBackAuth() = runTest {
        coEvery { authRepository.signInWithGoogle("token") } returns Result.success("uid-1")
        coEvery { userRepository.getOrCreateUser(any()) } returns
                Result.failure(RuntimeException("Firestore error"))

        viewModel.handleGoogleSignIn("token", "Test", "t@t.com", null)

        assertEquals(GoogleSignInStatus.ERROR, viewModel.uiState.value.status)
        coVerify { authRepository.signOut() }
    }

    @Test
    fun handleGoogleSignIn_createsUserWithCorrectDefaults() = runTest {
        coEvery { authRepository.signInWithGoogle("token") } returns Result.success("uid-new")
        coEvery { userRepository.getOrCreateUser(any()) } returns Result.success(
            User("uid-new", "Quân", "quan@test.com", "https://photo.url/q.jpg")
        )

        viewModel.handleGoogleSignIn("token", "Quân", "quan@test.com", "https://photo.url/q.jpg")

        coVerify {
            userRepository.getOrCreateUser(match { user ->
                user.userId == "uid-new" &&
                user.displayName == "Quân" &&
                user.email == "quan@test.com" &&
                user.photoUrl == "https://photo.url/q.jpg" &&
                user.level == SpeakingLevel.NOT_ASSESSED &&
                user.savedVocabularies.isEmpty()
            })
        }
    }

    @Test
    fun handleGoogleSignIn_nullDisplayNameAndEmail_defaultsToEmpty() = runTest {
        coEvery { authRepository.signInWithGoogle("token") } returns Result.success("uid-1")
        coEvery { userRepository.getOrCreateUser(any()) } returns Result.success(
            User("uid-1", "", "", "")
        )

        viewModel.handleGoogleSignIn("token", null, null, null)

        coVerify {
            userRepository.getOrCreateUser(match { user ->
                user.displayName == "" &&
                user.email == "" &&
                user.photoUrl == ""
            })
        }
    }

    @Test
    fun resetState_setsStatusNormal() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns
                Result.failure(RuntimeException("error"))

        viewModel.handleGoogleSignIn("token", null, null, null)
        assertEquals(GoogleSignInStatus.ERROR, viewModel.uiState.value.status)

        viewModel.resetState()
        assertEquals(GoogleSignInStatus.NORMAL, viewModel.uiState.value.status)
    }
}
