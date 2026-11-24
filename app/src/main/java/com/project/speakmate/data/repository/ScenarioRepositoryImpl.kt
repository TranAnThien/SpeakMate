package com.project.speakmate.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.ScenarioDto
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreDocumentId
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.isServerConfirmed
import com.project.speakmate.data.remote.firebase.toFirestoreSource
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.defaultEmpty
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScenarioRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore) : ScenarioRepository {
    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Scenario? =
        withContext(Dispatchers.IO) {
            if (!FirestoreDocumentId.isValid(scenarioId)) return@withContext null
            val document = getDocument(scenarioId, requireServer)
            val scenario = document.toObject(ScenarioDto::class.java)?.withDocumentId(document.id)?.toDomain()
            scenario?.takeIf { it.scenarioId == scenarioId }
        }

    override suspend fun getScenarioPreparationMetadata(
        scenarioId: String,
        requireServer: Boolean
    ): ScenarioPreparationMetadata =
        withContext(Dispatchers.IO) {
            val document = requireDocument(scenarioId, requireServer)
            ScenarioPreparationMapper.toMetadata(document.data.default(emptyMap()))
        }

    override suspend fun getScenarioImageUrl(scenarioId: String): String =
        withContext(Dispatchers.IO) { requireDocument(scenarioId).getString(FirestoreField.IMAGE_URL).defaultEmpty() }

    private suspend fun requireDocument(scenarioId: String, requireServer: Boolean = false): DocumentSnapshot {
        require(FirestoreDocumentId.isValid(scenarioId))
        val document = getDocument(scenarioId, requireServer)
        check(document.exists()) { "Scenario '$scenarioId' is missing from Firestore." }
        return document
    }

    private suspend fun getDocument(scenarioId: String, requireServer: Boolean): DocumentSnapshot =
        firestore
            .collection(FirestoreCollection.SCENARIOS)
            .document(scenarioId)
            .get(requireServer.toFirestoreSource())
            .await()
            .also { if (requireServer) check(it.metadata.isServerConfirmed()) }
}
