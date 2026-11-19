package com.project.speakmate.ui.call

import androidx.lifecycle.ViewModelStore
import com.project.speakmate.test.FakeAuthRepository
import com.project.speakmate.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallRecoveryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun dismissalKeepsTheRecoveryFactToPreventRestoringTheOldCallNavigation() = runTest {
        val repository = FakeCallRecoveryRepository()
        val auth = FakeAuthRepository()
        val viewModel = CallRecoveryViewModel(auth, repository)
        assertTrue(viewModel.uiState.value.isNoticeVisible)
        viewModel.dismissNotice()
        assertFalse(viewModel.uiState.value.isNoticeVisible)
        assertTrue(viewModel.uiState.value.wasInterrupted)
        assertEquals(listOf(auth.requireCurrentUserId()), repository.users)
        ViewModelStore().also { it.put(CallRecoveryViewModelTest::class.java.name, viewModel); it.clear() }
    }

    @Test
    fun aConsumedNoticeDoesNotAppearInANewStartupViewModel() = runTest {
        val repository = FakeCallRecoveryRepository()
        val auth = FakeAuthRepository()
        CallRecoveryViewModel(auth, repository).dismissNotice()
        val next = CallRecoveryViewModel(auth, repository)
        assertFalse(next.uiState.value.isNoticeVisible)
        assertFalse(next.uiState.value.isLoading)
    }

    @Test
    fun signingOutHidesThePreviousUsersNotice() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = CallRecoveryViewModel(auth, FakeCallRecoveryRepository())
        auth.signedInUserId.value = null
        runCurrent()
        assertFalse(viewModel.uiState.value.isNoticeVisible)
        assertEquals(null, viewModel.uiState.value.userId)
    }

    @Test
    fun storageFailureDoesNotBlockStartup() = runTest {
        val repository = FakeCallRecoveryRepository().also { it.failure = IllegalStateException() }
        val viewModel = CallRecoveryViewModel(FakeAuthRepository(), repository)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isNoticeVisible)
    }
}
