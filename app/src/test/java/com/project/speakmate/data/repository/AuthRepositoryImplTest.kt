package com.project.speakmate.data.repository

import com.project.speakmate.data.local.ActiveCallLocalDataSource
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var authDataSource: FirebaseAuthDataSource
    private lateinit var callMarkers: ActiveCallLocalDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authDataSource = mockk()
        callMarkers = mockk(relaxed = true)
        every { authDataSource.requireCurrentUserId() } returns "uid-current"
        repository =
            AuthRepositoryImpl(
                authDataSource = authDataSource,
                callMarkers = callMarkers,
                networkStatus = mockk(relaxed = true)
            )
    }

    @Test
    fun signInWithGoogleReturnsSuccessWithUid() = runTest {
        coEvery { authDataSource.signInWithGoogle("valid-token") } returns "uid-abc"

        val result = repository.signInWithGoogle("valid-token")

        assertTrue(result.isSuccess)
        assertEquals("uid-abc", result.getOrNull())
    }

    @Test
    fun signInWithGoogleReturnsFailureWhenAuthFails() = runTest {
        coEvery { authDataSource.signInWithGoogle("bad-token") } throws RuntimeException("Auth failed")

        val result = repository.signInWithGoogle("bad-token")

        assertTrue(result.isFailure)
        assertEquals("Auth failed", result.exceptionOrNull()!!.message)
    }

    @Test
    fun signOutReturnsSuccess() = runTest {
        coEvery { authDataSource.signOut() } just runs

        val result = repository.signOut()

        assertTrue(result.isSuccess)
    }

    @Test
    fun signOutRemovesOnlySignedInUserMarker() = runTest {
        coEvery { authDataSource.signOut() } just runs

        val result = repository.signOut()

        assertTrue(result.isSuccess)
        verify(exactly = 1) { callMarkers.removeUserCall("uid-current") }
        verify(exactly = 0) { callMarkers.removeUserCall(neq("uid-current")) }
    }

    @Test
    fun signOutReturnsFailureWhenThrows() = runTest {
        coEvery { authDataSource.signOut() } throws RuntimeException("Sign out error")

        val result = repository.signOut()

        assertTrue(result.isFailure)
        assertEquals("Sign out error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getSignedInUserIdEmitsUid() = runTest {
        every { authDataSource.observeSignedInUserId() } returns flowOf("uid-123")

        val uid = repository.observeSignedInUserId().first()

        assertEquals("uid-123", uid)
    }

    @Test
    fun getSignedInUserIdEmitsNullWhenNotSignedIn() = runTest {
        every { authDataSource.observeSignedInUserId() } returns flowOf(null)

        val uid = repository.observeSignedInUserId().first()

        assertNull(uid)
    }
}
