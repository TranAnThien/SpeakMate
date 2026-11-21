package com.project.speakmate.data.local

import android.content.Context
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveCallLocalDataSourceTest {
    companion object {
        private const val USER_ID = "student"
        private const val OTHER_USER_ID = "other-student"
        private const val OLD_OWNER = "old-owner"
        private const val NEW_OWNER = "new-owner"
    }

    private val storage = FakeSharedPreferences()
    private val context = mockk<Context>()
    private val auth = mockk<FirebaseAuthDataSource>()
    private var signedInUser: String? = USER_ID

    @Test
    fun storageWorkIsQueuedAndCurrentProcessDoesNotReportItsActiveCall() = runTest {
        val source = source(backgroundScope)
        source.saveActiveCall(USER_ID, OLD_OWNER)
        assertEquals(0, storage.operations)
        runCurrent()
        assertEquals(1, storage.values.size)
        val result = async { source.consumeInterruptedCall(USER_ID) }
        runCurrent()
        assertFalse(result.await())
        assertEquals(1, storage.values.size)
    }

    @Test
    fun aNewProcessReportsThePreviousCallOnceAndConsumesTheMarker() = runTest {
        source(backgroundScope).saveActiveCall(USER_ID, OLD_OWNER)
        runCurrent()
        val restarted = source(backgroundScope)
        val first = async { restarted.consumeInterruptedCall(USER_ID) }
        runCurrent()
        assertTrue(first.await())
        val second = async { restarted.consumeInterruptedCall(USER_ID) }
        runCurrent()
        assertFalse(second.await())
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun oldOwnersCannotClearTheNewOwnersMarkerAndCleanupIsRepeatSafe() = runTest {
        val source = source(backgroundScope)
        source.saveActiveCall(USER_ID, OLD_OWNER)
        source.saveActiveCall(USER_ID, NEW_OWNER)
        source.removeActiveCall(OLD_OWNER)
        source.removeActiveCall(OLD_OWNER)
        runCurrent()
        assertTrue(storage.values.values.single().contains(NEW_OWNER))
        source.removeActiveCall(NEW_OWNER)
        source.removeActiveCall(NEW_OWNER)
        runCurrent()
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun accountsCannotConsumeEachOthersInterruptedCall() = runTest {
        source(backgroundScope).saveActiveCall(USER_ID, OLD_OWNER)
        runCurrent()
        signedInUser = OTHER_USER_ID
        val source = source(backgroundScope)
        val result = async { source.consumeInterruptedCall(OTHER_USER_ID) }
        runCurrent()
        assertFalse(result.await())
        assertEquals(1, storage.values.size)
    }

    @Test
    fun signOutCleanupRemovesOnlyThatUsersMarkerAndLateWritesAreIgnored() = runTest {
        val source = source(backgroundScope)
        source.saveActiveCall(USER_ID, OLD_OWNER)
        runCurrent()
        signedInUser = null
        source.removeUserCall(USER_ID)
        source.saveActiveCall(USER_ID, NEW_OWNER)
        runCurrent()
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun ioErrorsDoNotEscapeAndRecoveryDoesNotHang() = runTest {
        storage.shouldFail = true
        val source = source(backgroundScope)
        source.saveActiveCall(USER_ID, OLD_OWNER)
        source.removeActiveCall(OLD_OWNER)
        source.removeUserCall(USER_ID)
        val result = async { source.consumeInterruptedCall(USER_ID) }
        runCurrent()
        assertFalse(result.await())
    }

    private fun source(scope: CoroutineScope): ActiveCallLocalDataSource {
        every { context.getSharedPreferences(any(), any()) } returns storage.preferences
        every { auth.requireCurrentUserId() } answers { checkNotNull(signedInUser) }
        return ActiveCallLocalDataSource(context, scope, auth)
    }
}
