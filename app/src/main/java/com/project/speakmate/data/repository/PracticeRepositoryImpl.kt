package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.PracticeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class PracticeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PracticeRepository {
    private val freePractices get() = firestore.collection("freePractices")
    private val scenarios get() = firestore.collection("scenarios")

    override suspend fun getScenarioCatalog(): Result<List<FreePractice>> = runCatching {
        freePractices.get().await().documents.map { document ->
            FreePractice(
                practiceId = document.getString("practiceId")?.takeIf(String::isNotBlank) ?: document.id,
                scenarioId = document.getString("scenarioId")?.takeIf(String::isNotBlank)
                    ?: error("FreePractice '${document.id}' is missing scenarioId."),
                category = document.getString("category")?.takeIf(String::isNotBlank)
                    ?: error("FreePractice '${document.id}' is missing category.")
            )
        }.sortedBy(FreePractice::category)
    }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = runCatching {
        val document = scenarios.document(scenarioId).get().await()
        require(document.exists()) { "Scenario '$scenarioId' is missing from Firestore." }
        Scenario(
            scenarioId = document.getString("scenarioId")?.takeIf(String::isNotBlank) ?: document.id,
            name = document.getString("name")?.takeIf(String::isNotBlank)
                ?: error("Scenario '$scenarioId' is missing name."),
            description = document.getString("description")?.takeIf(String::isNotBlank)
                ?: error("Scenario '$scenarioId' is missing description."),
            promptTemplate = document.getString("promptTemplate")?.takeIf(String::isNotBlank)
                ?: error("Scenario '$scenarioId' is missing promptTemplate.")
        )
    }

    override suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<Map<String, String>> = runCatching {
        val document = scenarios.document(scenarioId).get().await()
        require(document.exists()) { "Scenario '$scenarioId' is missing from Firestore." }
        mapOf(
            "imageUrl" to document.requiredPreparationText("imageUrl"),
            "uiContext" to document.requiredPreparationText("uiContext"),
            "aiRole" to document.requiredPreparationText("aiRole"),
            "preCallHint" to document.requiredPreparationText("preCallHint")
        )
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.requiredPreparationText(field: String): String =
    getString(field)?.takeIf(String::isNotBlank)
        ?: error("Scenario '$id' is missing preparation field '$field'.")
