package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.ScenarioDto
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.requiredPreparationText
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.ScenarioRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScenarioRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore) : ScenarioRepository {
    companion object {
        private const val PATH_SEPARATOR = '/'
    }

    override suspend fun getScenario(scenarioId: String): Scenario? = withContext(Dispatchers.IO) {
        if (scenarioId.isBlank() || PATH_SEPARATOR in scenarioId) return@withContext null
        val document = getDocument(scenarioId)
        val scenario = document.toObject(ScenarioDto::class.java)?.withDocumentId(document.id)?.toDomain()
        scenario?.takeIf { it.scenarioId == scenarioId }
    }
    override suspend fun getScenarioPreparationMetadata(scenarioId: String): ScenarioPreparationMetadata =
        withContext(Dispatchers.IO) {
            val document = requireDocument(scenarioId)
            ScenarioPreparationMetadata(
                imageUrl = document.requiredPreparationText(FirestoreField.IMAGE_URL),
                uiContext = document.requiredPreparationText(FirestoreField.UI_CONTEXT),
                aiRole = document.requiredPreparationText(FirestoreField.AI_ROLE),
                preCallHint = document.requiredPreparationText(FirestoreField.PRE_CALL_HINT)
            )
        }

    override suspend fun getScenarioImageUrl(scenarioId: String): String = withContext(Dispatchers.IO) {
        requireDocument(scenarioId).getString(FirestoreField.IMAGE_URL).orEmpty()
    }

    private suspend fun requireDocument(scenarioId: String): DocumentSnapshot {
        require(scenarioId.isNotBlank() && PATH_SEPARATOR !in scenarioId)
        val document = getDocument(scenarioId)
        check(document.exists()) { "Scenario '$scenarioId' is missing from Firestore." }
        return document
    }

    private suspend fun getDocument(scenarioId: String): DocumentSnapshot =
        firestore.collection(FirestoreCollection.SCENARIOS).document(scenarioId).get().await()
}
