package com.project.speakmate.ui.profile

import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(authRepository, userRepository)
    }

    @Test
    fun initialState_userProfileIsNull() {
        every { authRepository.getSignedInUserId() } returns flowOf(null)

        val viewModel = createViewModel()

        assertNull(viewModel.userProfile.value)
    }

    @Test
    fun observeCurrentUser_setsUserProfile_whenUidAndDocumentExist() = runTest {
        val user = User("uid-1", "Quân", "quan@test.com", "https://photo.url/q.jpg", SpeakingLevel.A1)
        every { authRepository.getSignedInUserId() } returns flowOf("uid-1")
        coEvery { userRepository.getUser("uid-1") } returns Result.success(user)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.userProfile.value)
        assertEquals("uid-1", viewModel.userProfile.value!!.userId)
        assertEquals("Quân", viewModel.userProfile.value!!.displayName)
        assertEquals(SpeakingLevel.A1, viewModel.userProfile.value!!.level)
    }

    @Test
    fun observeCurrentUser_forceSignOut_whenUidExistsButNoDocument() = runTest {
        every { authRepository.getSignedInUserId() } returns flowOf("uid-orphan")
        coEvery { userRepository.getUser("uid-orphan") } returns Result.success(null)

        createViewModel()

        advanceUntilIdle()

        coVerify { authRepository.signOut() }
    }

    @Test
    fun observeCurrentUser_forceSignOut_whenFirestoreFails() = runTest {
        every { authRepository.getSignedInUserId() } returns flowOf("uid-err")
        coEvery { userRepository.getUser("uid-err") } returns
                Result.failure(RuntimeException("Network error"))

        createViewModel()

        advanceUntilIdle()

        coVerify { authRepository.signOut() }
    }

    @Test
    fun observeCurrentUser_setsNull_whenUidIsNull() = runTest {
        every { authRepository.getSignedInUserId() } returns flowOf(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.userProfile.value)
    }

    @Test
    fun observeCurrentUser_updatesProfile_whenUidChanges() = runTest {
        val uidFlow = MutableSharedFlow<String?>(replay = 1)
        every { authRepository.getSignedInUserId() } returns uidFlow

        val userA = User("uid-a", "User A", "a@test.com", "", SpeakingLevel.A1)
        val userB = User("uid-b", "User B", "b@test.com", "", SpeakingLevel.B2)
        coEvery { userRepository.getUser("uid-a") } returns Result.success(userA)
        coEvery { userRepository.getUser("uid-b") } returns Result.success(userB)

        val viewModel = createViewModel()

        uidFlow.emit("uid-a")
        advanceUntilIdle()
        assertEquals("User A", viewModel.userProfile.value!!.displayName)

        uidFlow.emit("uid-b")
        advanceUntilIdle()
        assertEquals("User B", viewModel.userProfile.value!!.displayName)
        assertEquals(SpeakingLevel.B2, viewModel.userProfile.value!!.level)
    }

    @Test
    fun signOut_callsAuthRepositorySignOut() = runTest {
        every { authRepository.getSignedInUserId() } returns flowOf(null)

        val viewModel = createViewModel()
        var callbackInvoked = false

        viewModel.signOut { callbackInvoked = true }
        advanceUntilIdle()

        coVerify { authRepository.signOut() }
        assertTrue(callbackInvoked)
    }

    @Test
    fun signOut_invokesCallback_afterSignOut() = runTest {
        every { authRepository.getSignedInUserId() } returns flowOf("uid-1")
        coEvery { userRepository.getUser("uid-1") } returns Result.success(
            User("uid-1", "Test", "t@t.com", "")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val callOrder = mutableListOf<String>()
        coEvery { authRepository.signOut() } answers {
            callOrder.add("signOut")
            Result.success(Unit)
        }

        viewModel.signOut {
            callOrder.add("callback")
        }
        advanceUntilIdle()

        assertEquals(listOf("signOut", "callback"), callOrder)
    }
}
