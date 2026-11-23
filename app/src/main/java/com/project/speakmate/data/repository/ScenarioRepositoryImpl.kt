package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.ScenarioDto
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.defaultEmpty
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

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Scenario? = withContext(Dispatchers.IO) {
        if (scenarioId.isBlank() || PATH_SEPARATOR in scenarioId) return@withContext null
        val document = getDocument(scenarioId, requireServer)
        val scenario = document.toObject(ScenarioDto::class.java)?.withDocumentId(document.id)?.toDomain()
        scenario?.takeIf { it.scenarioId == scenarioId }
    }
    override suspend fun getScenarioPreparationMetadata(scenarioId: String, requireServer: Boolean): ScenarioPreparationMetadata =
        withContext(Dispatchers.IO) {
            val document = requireDocument(scenarioId, requireServer)
            ScenarioPreparationMapper.toMetadata(document.data.default(emptyMap()))
        }

    override suspend fun getScenarioImageUrl(scenarioId: String): String = withContext(Dispatchers.IO) {
        requireDocument(scenarioId).getString(FirestoreField.IMAGE_URL).defaultEmpty()
    }

    private suspend fun requireDocument(scenarioId: String, requireServer: Boolean = false): DocumentSnapshot {
        require(scenarioId.isNotBlank() && PATH_SEPARATOR !in scenarioId)
        val document = getDocument(scenarioId, requireServer)
        check(document.exists()) { "Scenario '$scenarioId' is missing from Firestore." }
        return document
    }

    private suspend fun getDocument(scenarioId: String, requireServer: Boolean): DocumentSnapshot =
        firestore.collection(FirestoreCollection.SCENARIOS).document(scenarioId).get(if (requireServer) Source.SERVER else Source.DEFAULT).await().also {
            if (requireServer) check(!it.metadata.isFromCache && !it.metadata.hasPendingWrites())
        }
}
