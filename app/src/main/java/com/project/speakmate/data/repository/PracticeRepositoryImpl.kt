package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class PracticeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val scenarioRepository: ScenarioRepository
) : PracticeRepository {
    private val freePractices get() = firestore.collection(FirestoreCollection.FREE_PRACTICES)

    override suspend fun getScenarioCatalog(): Result<List<FreePractice>> = suspendRunCatching {
        freePractices.get().await().documents.map { document ->
            FreePractice(
                practiceId = document.getString(FirestoreField.PRACTICE_ID)?.takeIf(String::isNotBlank).default(document.id),
                scenarioId = document.getString(FirestoreField.SCENARIO_ID)?.takeIf(String::isNotBlank)
                    ?: error("FreePractice '${document.id}' is missing scenarioId."),
                category = document.getString(FirestoreField.CATEGORY)?.takeIf(String::isNotBlank)
                    ?: error("FreePractice '${document.id}' is missing category.")
            )
        }.sortedBy(FreePractice::category)
    }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = suspendRunCatching {
        checkNotNull(scenarioRepository.getScenario(scenarioId)) { "Scenario '$scenarioId' is missing or invalid." }
    }

    override suspend fun getScenarioPreparationMetadata(scenarioId: String): Result<ScenarioPreparationMetadata> =
        suspendRunCatching { scenarioRepository.getScenarioPreparationMetadata(scenarioId) }
}
