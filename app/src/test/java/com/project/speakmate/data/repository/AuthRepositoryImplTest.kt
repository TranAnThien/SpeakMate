package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var authDataSource: FirebaseAuthDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authDataSource = mockk()
        repository = AuthRepositoryImpl(authDataSource)
    }

    @Test
    fun signInWithGoogle_returnsSuccess_withUid() = runTest {
        coEvery { authDataSource.signInWithGoogle("valid-token") } returns "uid-abc"

        val result = repository.signInWithGoogle("valid-token")

        assertTrue(result.isSuccess)
        assertEquals("uid-abc", result.getOrNull())
    }

    @Test
    fun signInWithGoogle_returnsFailure_whenAuthFails() = runTest {
        coEvery { authDataSource.signInWithGoogle("bad-token") } throws RuntimeException("Auth failed")

        val result = repository.signInWithGoogle("bad-token")

        assertTrue(result.isFailure)
        assertEquals("Auth failed", result.exceptionOrNull()!!.message)
    }

    @Test
    fun signOut_returnsSuccess() = runTest {
        every { authDataSource.signOut() } just runs

        val result = repository.signOut()

        assertTrue(result.isSuccess)
    }

    @Test
    fun signOut_returnsFailure_whenThrows() = runTest {
        every { authDataSource.signOut() } throws RuntimeException("Sign out error")

        val result = repository.signOut()

        assertTrue(result.isFailure)
        assertEquals("Sign out error", result.exceptionOrNull()!!.message)
    }

    @Test
    fun getSignedInUserId_emitsUid() = runTest {
        every { authDataSource.getSignedInUserId() } returns flowOf("uid-123")

        val uid = repository.getSignedInUserId().first()

        assertEquals("uid-123", uid)
    }

    @Test
    fun getSignedInUserId_emitsNull_whenNotSignedIn() = runTest {
        every { authDataSource.getSignedInUserId() } returns flowOf(null)

        val uid = repository.getSignedInUserId().first()

        assertNull(uid)
    }
}
