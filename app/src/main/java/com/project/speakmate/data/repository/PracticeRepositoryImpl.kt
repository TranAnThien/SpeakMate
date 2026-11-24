package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.isServerConfirmed
import com.project.speakmate.data.remote.firebase.toFirestoreSource
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.ScenarioPreparationMetadata
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class PracticeRepositoryImpl
@Inject
constructor(private val firestore: FirebaseFirestore, private val scenarioRepository: ScenarioRepository) :
    PracticeRepository {
    private val freePractices
        get() = firestore.collection(FirestoreCollection.FREE_PRACTICES)

    override suspend fun getScenarioCatalog(requireServer: Boolean): Result<List<FreePractice>> =
        withContext(Dispatchers.IO) {
            suspendRunCatching {
                val source = requireServer.toFirestoreSource()
                val snapshot = freePractices.get(source).await()
                if (requireServer) {
                    check(snapshot.metadata.isServerConfirmed())
                }
                snapshot.documents
                    .mapNotNull { document ->
                        try {
                            val scenarioId =
                                document.getString(FirestoreField.SCENARIO_ID)?.takeIf(String::isNotBlank)
                                    ?: return@mapNotNull null
                            val category =
                                document.getString(FirestoreField.CATEGORY)?.takeIf(String::isNotBlank)
                                    ?: return@mapNotNull null
                            FreePractice(
                                practiceId =
                                    document
                                        .getString(FirestoreField.PRACTICE_ID)
                                        ?.takeIf(String::isNotBlank)
                                        .default(document.id),
                                scenarioId = scenarioId,
                                category = category
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: RuntimeException) {
                            null
                        }
                    }
                    .sortedBy(FreePractice::category)
            }
        }

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> =
        suspendRunCatching {
            checkNotNull(scenarioRepository.getScenario(scenarioId, requireServer)) {
                "Scenario '$scenarioId' is missing or invalid."
            }
        }

    override suspend fun getScenarioPreparationMetadata(
        scenarioId: String,
        requireServer: Boolean
    ): Result<ScenarioPreparationMetadata> = suspendRunCatching {
        scenarioRepository.getScenarioPreparationMetadata(scenarioId, requireServer)
    }
}
