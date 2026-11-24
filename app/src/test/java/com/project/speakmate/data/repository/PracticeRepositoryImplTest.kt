package com.project.speakmate.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.repository.ScenarioRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PracticeRepositoryImplTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val task = mockk<Task<QuerySnapshot>>()
    private val snapshot = mockk<QuerySnapshot>()
    private val scenarioRepository = mockk<ScenarioRepository>()
    private val repository = PracticeRepositoryImpl(firestore, scenarioRepository)

    init {
        every { firestore.collection("freePractices") } returns collection
        every { collection.get(any<Source>()) } returns task
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.exception } returns null
        every { task.result } returns snapshot
    }

    @Test
    fun catalogUsesConfiguredIdsAndSortsByCategoryWithoutDroppingEntries() = runTest {
        every { snapshot.documents } returns
            listOf(
                document(id = "second", practiceId = "stored", scenario = "s2", category = "Travel"),
                document(id = "first", practiceId = "entry", scenario = "s1", category = "Cafe")
            )
        assertEquals(
            listOf(
                FreePractice(practiceId = "entry", scenarioId = "s1", category = "Cafe"),
                FreePractice(practiceId = "stored", scenarioId = "s2", category = "Travel")
            ),
            repository.getScenarioCatalog().getOrThrow()
        )
    }

    @Test
    fun missingAndBlankPracticeIdsUseTheDocumentId() = runTest {
        every { snapshot.documents } returns
            listOf(
                document(id = "missing", practiceId = null, scenario = "s1", category = "Cafe"),
                document(id = "blank", practiceId = " ", scenario = "s2", category = "Cafe")
            )
        assertEquals(listOf("missing", "blank"), repository.getScenarioCatalog().getOrThrow().map { it.practiceId })
    }

    @Test
    fun missingScenarioOrCategoryIsSkippedWhileValidEntriesRemain() = runTest {
        listOf(null to "Cafe", " " to "Cafe", "scenario" to null, "scenario" to " ").forEach { (scenario, category) ->
            every { snapshot.documents } returns
                listOf(
                    document(id = "valid", practiceId = "valid", scenario = "scenario", category = "Cafe"),
                    document(id = "bad", practiceId = null, scenario = scenario, category = category)
                )
            assertEquals(
                listOf(FreePractice(practiceId = "valid", scenarioId = "scenario", category = "Cafe")),
                repository.getScenarioCatalog().getOrThrow()
            )
        }
    }

    @Test
    fun emptyConfiguredCatalogIsAnEmptySuccess() = runTest {
        every { snapshot.documents } returns emptyList()
        assertEquals(emptyList<FreePractice>(), repository.getScenarioCatalog().getOrThrow())
        verify(exactly = 1) { collection.get(Source.DEFAULT) }
    }

    @Test
    fun serverCatalogUsesServerSourceAndConfirmedMetadata() = runTest {
        every { snapshot.documents } returns emptyList()
        every { snapshot.metadata } returns
            mockk {
                every { isFromCache } returns false
                every { hasPendingWrites() } returns false
            }

        assertEquals(emptyList<FreePractice>(), repository.getScenarioCatalog(requireServer = true).getOrThrow())
        verify(exactly = 1) { collection.get(Source.SERVER) }
        verify(exactly = 0) { collection.get(Source.DEFAULT) }
    }

    @Test
    fun firestoreFailureIsReturnedWithoutDefaultCatalogData() = runTest {
        val failure = IllegalStateException("read failed")
        every { task.exception } returns failure
        assertSame(failure, repository.getScenarioCatalog().exceptionOrNull())
    }

    @Test
    fun cancelledCatalogTaskPropagatesCancellation() {
        every { task.isCanceled } returns true
        assertThrows(CancellationException::class.java) { runTest { repository.getScenarioCatalog() } }
    }

    private fun document(id: String, practiceId: String?, scenario: String?, category: String?): DocumentSnapshot =
        mockk<DocumentSnapshot>().also { document ->
            every { document.id } returns id
            every { document.getString("practiceId") } returns practiceId
            every { document.getString("scenarioId") } returns scenario
            every { document.getString("category") } returns category
        }
}
