package com.project.speakmate.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.FirestoreTrainingDataSource
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.repository.ScenarioRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingRepositoryImplTest {
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val scenarioRepository = mockk<ScenarioRepository>()
    private val repository =
        TrainingRepositoryImpl(
            dataSource = FirestoreTrainingDataSource(firestore),
            scenarioRepository = scenarioRepository,
            contentMapper = TrainingContentMapper()
        )

    @Test
    fun malformedPreparationItemIsSkippedWithoutDroppingValidItems() = runTest {
        configureUnit()
        configureVocabulary(id = "valid", fields = TrainingContentTestFixtures.getVocabulary())
        configureVocabulary(id = "invalid", fields = emptyMap())

        val unit = repository.getTrainingPath(SpeakingLevel.A2).getOrThrow().goals.single().units.single()

        assertEquals(listOf("coffee"), unit.preparation.vocabulary.map { it.word })
    }

    @Test
    fun missingPreparationDocumentIsSkippedWithoutFailingThePath() = runTest {
        configureUnit()
        configureVocabulary(id = "valid", fields = TrainingContentTestFixtures.getVocabulary())
        val missing: DocumentSnapshot = TrainingContentTestFixtures.getDocument(emptyMap())
        every { missing.exists() } returns false
        every { firestore.collection("vocabularies").document("invalid").get() } returns Tasks.forResult(missing)

        val unit = repository.getTrainingPath(SpeakingLevel.A2).getOrThrow().goals.single().units.single()

        assertEquals(listOf("coffee"), unit.preparation.vocabulary.map { it.word })
    }

    @Test
    fun preparationReadFailurePropagatesInsteadOfPublishingPartialContent() = runTest {
        configureUnit()
        configureVocabulary(id = "valid", fields = TrainingContentTestFixtures.getVocabulary())
        val failure = IllegalStateException("network read failed")
        every { firestore.collection("vocabularies").document("invalid").get() } returns Tasks.forException(failure)

        val observed = checkNotNull(repository.getTrainingPath(SpeakingLevel.A2).exceptionOrNull())
        assertEquals(failure::class, observed::class)
        assertEquals(failure.message, observed.message)
    }

    private fun configurePath() {
        val path =
            TrainingContentTestFixtures.getDocument(
                mapOf(
                    FirestoreField.PATH_ID to "path",
                    FirestoreField.TITLE to "Path",
                    FirestoreField.TARGET_LEVEL to "A2",
                    FirestoreField.GOALS to listOf("goal")
                )
            )
        val goal =
            TrainingContentTestFixtures.getDocument(
                mapOf(
                    FirestoreField.GOAL_ID to "goal",
                    FirestoreField.TITLE to "Goal",
                    FirestoreField.CAN_DO_DESCRIPTION to "Order",
                    FirestoreField.UNITS to listOf("unit"),
                    FirestoreField.GOAL_ASSESSMENT to "assessment"
                )
            )
        val assessment =
            TrainingContentTestFixtures.getDocument(
                mapOf(
                    FirestoreField.SCENARIO_ID to "scenario",
                    FirestoreField.TITLE to "Assessment",
                    FirestoreField.REQUIRED_ACTIONS to emptyList<Any>()
                )
            )
        listOf(path, goal, assessment).forEach { document -> every { document.exists() } returns true }
        every { firestore.collection(FirestoreCollection.TRAINING_PATHS).document("A2").get(any<Source>()) } returns
            Tasks.forResult(path)
        every { firestore.collection(FirestoreCollection.TRAINING_GOALS).document("goal").get(any<Source>()) } returns
            Tasks.forResult(goal)
        every {
            firestore.collection(FirestoreCollection.GOAL_ASSESSMENTS).document("assessment").get(any<Source>())
        } returns Tasks.forResult(assessment)
    }

    private fun configureUnit() {
        configurePath()
        val unit =
            TrainingContentTestFixtures.getDocument(
                mapOf(
                    "unitId" to "unit",
                    FirestoreField.SCENARIO_ID to "scenario",
                    FirestoreField.TITLE to "Unit",
                    "communicativeFocus" to "Order",
                    FirestoreField.REQUIRED_ACTIONS to emptyList<Any>(),
                    "preparation" to mapOf("vocabulary" to listOf(mapOf("id" to "valid"), mapOf("id" to "invalid")))
                )
            )
        every { unit.exists() } returns true
        every { firestore.collection("training_units").document("unit").get(any<Source>()) } returns
            Tasks.forResult(unit)
    }

    private fun configureVocabulary(id: String, fields: Map<String, Any?>) {
        val document: DocumentSnapshot = TrainingContentTestFixtures.getDocument(fields)
        every { document.exists() } returns true
        every { firestore.collection("vocabularies").document(id).get() } returns Tasks.forResult(document)
    }
}
