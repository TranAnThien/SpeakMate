package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.FirestoreTrainingDataSource
import com.project.speakmate.data.remote.firebase.requiredActions
import com.project.speakmate.data.remote.firebase.requiredLevel
import com.project.speakmate.data.remote.firebase.requiredString
import com.project.speakmate.data.remote.firebase.requiredStringList
import com.project.speakmate.data.remote.firebase.stringOrId
import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.GrammarPoint
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SentencePattern
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.ScenarioRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.utils.extension.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class TrainingRepositoryImpl
@Inject
constructor(
    private val dataSource: FirestoreTrainingDataSource,
    private val scenarioRepository: ScenarioRepository,
    private val contentMapper: TrainingContentMapper
) : TrainingRepository {

    override suspend fun getTrainingPath(level: SpeakingLevel, requireServer: Boolean): Result<TrainingPath> =
        suspendRunCatching {
            loadPath(level = level, requireServer = requireServer, includePreparation = true)
        }

    override suspend fun getTrainingPathStructure(level: SpeakingLevel): Result<TrainingPath> = suspendRunCatching {
        loadPath(level = level, requireServer = true, includePreparation = false)
    }

    private suspend fun loadPath(
        level: SpeakingLevel,
        requireServer: Boolean,
        includePreparation: Boolean
    ): TrainingPath {
        require(level != SpeakingLevel.NOT_ASSESSED) { "A training path requires an assessed level." }
        val document = dataSource.getTrainingPath(level, requireServer)
        val goalIds = document.requiredStringList(FirestoreField.GOALS)
        return TrainingPath(
            pathId = document.stringOrId(FirestoreField.PATH_ID),
            targetLevel = document.requiredLevel(FirestoreField.TARGET_LEVEL),
            title = document.requiredString(FirestoreField.TITLE),
            goals =
                coroutineScope {
                    goalIds
                        .map { id ->
                            async {
                                loadGoal(
                                    goalId = id,
                                    requireServer = requireServer,
                                    includePreparation = includePreparation
                                )
                            }
                        }
                        .awaitAll()
                }
        )
    }

    override suspend fun getScenario(scenarioId: String, requireServer: Boolean): Result<Scenario> =
        suspendRunCatching {
            checkNotNull(scenarioRepository.getScenario(scenarioId, requireServer)) {
                "Scenario '$scenarioId' is missing or invalid."
            }
        }

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> = suspendRunCatching {
        scenarioRepository.getScenarioImageUrl(scenarioId)
    }

    private suspend fun loadGoal(
        goalId: String,
        requireServer: Boolean = false,
        includePreparation: Boolean = true
    ): TrainingGoal {
        val document = dataSource.getTrainingGoal(goalId, requireServer)
        val unitIds = document.requiredStringList(FirestoreField.UNITS)
        return TrainingGoal(
            goalId = document.stringOrId(FirestoreField.GOAL_ID),
            title = document.requiredString(FirestoreField.TITLE),
            canDoDescription = document.requiredString(FirestoreField.CAN_DO_DESCRIPTION),
            goalAssessment = loadAssessment(document.requiredString(FirestoreField.GOAL_ASSESSMENT), requireServer),
            units =
                coroutineScope {
                    unitIds
                        .map { id ->
                            async {
                                loadUnit(
                                    unitId = id,
                                    requireServer = requireServer,
                                    includePreparation = includePreparation
                                )
                            }
                        }
                        .awaitAll()
                }
        )
    }

    private suspend fun loadAssessment(assessmentId: String, requireServer: Boolean = false): GoalAssessment {
        val document = dataSource.getAssessment(assessmentId, requireServer)
        return contentMapper.toAssessment(document)
    }

    private suspend fun loadUnit(
        unitId: String,
        requireServer: Boolean = false,
        includePreparation: Boolean = true
    ): TrainingUnit {
        val document = dataSource.getTrainingUnit(unitId, requireServer)
        val loadedPreparation =
            if (includePreparation) loadPreparation(document.get(FirestoreField.PREPARATION))
            else UnitPreparation(vocabulary = emptyList(), grammarPoints = emptyList(), sentencePatterns = emptyList())
        return TrainingUnit(
            unitId = document.stringOrId(FirestoreField.UNIT_ID),
            scenarioId = document.requiredString(FirestoreField.SCENARIO_ID),
            title = document.requiredString(FirestoreField.TITLE),
            communicativeFocus = document.requiredString(FirestoreField.COMMUNICATIVE_FOCUS),
            preparation = loadedPreparation,
            requiredActions = document.requiredActions()
        )
    }

    private suspend fun loadPreparation(preparation: Any?): UnitPreparation {
        val vocabularyIds = contentMapper.getPreparationReferenceIds(preparation, FirestoreField.VOCABULARY)
        val grammarIds = contentMapper.getPreparationReferenceIds(preparation, FirestoreField.GRAMMAR_POINTS)
        val patternIds = contentMapper.getPreparationReferenceIds(preparation, FirestoreField.SENTENCE_PATTERNS)
        return coroutineScope {
            val vocabulary = async { vocabularyIds.mapNotNull { id -> loadVocabulary(id) } }
            val grammar = async { grammarIds.mapNotNull { id -> loadGrammar(id) } }
            val patterns = async { patternIds.mapNotNull { id -> loadPattern(id) } }
            UnitPreparation(
                vocabulary = vocabulary.await(),
                grammarPoints = grammar.await(),
                sentencePatterns = patterns.await()
            )
                .also(contentMapper::validatePreparation)
        }
    }

    private suspend fun loadVocabulary(id: String): VocabularyItem? {
        val document = dataSource.getVocabulary(id) ?: return null
        return try {
            contentMapper.toVocabulary(document, id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private suspend fun loadGrammar(id: String): GrammarPoint? {
        val document = dataSource.getGrammar(id) ?: return null
        return try {
            contentMapper.toGrammar(document, id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private suspend fun loadPattern(id: String): SentencePattern? {
        val document = dataSource.getPattern(id) ?: return null
        return try {
            contentMapper.toPattern(document, id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
