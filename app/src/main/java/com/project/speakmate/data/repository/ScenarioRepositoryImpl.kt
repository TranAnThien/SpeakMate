package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.dto.ScenarioDto
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.repository.ScenarioRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScenarioRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore) : ScenarioRepository {
    companion object {
        private const val COLLECTION_NAME = "scenarios"
        private const val PATH_SEPARATOR = '/'
    }

    override suspend fun getScenario(scenarioId: String): Scenario? = withContext(Dispatchers.IO) {
        if (scenarioId.isBlank() || PATH_SEPARATOR in scenarioId) return@withContext null
        val document = firestore.collection(COLLECTION_NAME).document(scenarioId).get().await()
        val scenario = document.toObject(ScenarioDto::class.java)?.withDocumentId(document.id)?.toDomain()
        scenario?.takeIf { it.scenarioId == scenarioId }
    }
}
