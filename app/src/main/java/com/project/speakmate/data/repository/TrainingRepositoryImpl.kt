package com.project.speakmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.FirestoreTrainingDataSource
import com.project.speakmate.data.remote.firebase.requiredActions
import com.project.speakmate.data.remote.firebase.requiredLevel
import com.project.speakmate.data.remote.firebase.requiredMap
import com.project.speakmate.data.remote.firebase.requiredReferenceIds
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val scenarioRepository: ScenarioRepository
) : TrainingRepository {
    private val dataSource = FirestoreTrainingDataSource(firestore)
    private val contentMapper = TrainingContentMapper()

    override suspend fun getTrainingPath(level: SpeakingLevel): Result<TrainingPath> = suspendRunCatching {
        require(level != SpeakingLevel.NOT_ASSESSED) { "A training path requires an assessed level." }
        val document = dataSource.getTrainingPath(level)
        val goalIds = document.requiredStringList(FirestoreField.GOALS)
        TrainingPath(
            pathId = document.stringOrId(FirestoreField.PATH_ID),
            targetLevel = document.requiredLevel(FirestoreField.TARGET_LEVEL),
            title = document.requiredString(FirestoreField.TITLE),
            goals = coroutineScope { goalIds.map { id -> async { loadGoal(id) } }.awaitAll() }
        )
    }

    override suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal> = suspendRunCatching { loadGoal(goalId) }

    override suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit> = suspendRunCatching {
        loadUnit(unitId, loadPreparation = true)
    }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = suspendRunCatching {
        checkNotNull(scenarioRepository.getScenario(scenarioId)) { "Scenario '$scenarioId' is missing or invalid." }
    }

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> =
        suspendRunCatching { scenarioRepository.getScenarioImageUrl(scenarioId) }

    private suspend fun loadGoal(goalId: String): TrainingGoal {
        val document = dataSource.getTrainingGoal(goalId)
        val unitIds = document.requiredStringList(FirestoreField.UNITS)
        return TrainingGoal(
            goalId = document.stringOrId(FirestoreField.GOAL_ID),
            title = document.requiredString(FirestoreField.TITLE),
            canDoDescription = document.requiredString(FirestoreField.CAN_DO_DESCRIPTION),
            goalAssessment = loadAssessment(document.requiredString(FirestoreField.GOAL_ASSESSMENT)),
            units = coroutineScope { unitIds.map { id -> async { loadUnit(id, loadPreparation = false) } }.awaitAll() }
        )
    }

    private suspend fun loadAssessment(assessmentId: String): GoalAssessment {
        val document = dataSource.getAssessment(assessmentId)
        return contentMapper.toAssessment(document)
    }

    private suspend fun loadUnit(unitId: String, loadPreparation: Boolean = true): TrainingUnit {
        val document = dataSource.getTrainingUnit(unitId)
        val loadedPreparation = if (loadPreparation) {
            val preparation = document.requiredMap(FirestoreField.PREPARATION)
            val vocabularyIds = preparation.requiredReferenceIds(FirestoreField.VOCABULARY)
            val grammarIds = preparation.requiredReferenceIds(FirestoreField.GRAMMAR_POINTS)
            val patternIds = preparation.requiredReferenceIds(FirestoreField.SENTENCE_PATTERNS)
            coroutineScope {
                val vocabulary = async { vocabularyIds.map { id -> loadVocabulary(id) } }
                val grammar = async { grammarIds.map { id -> loadGrammar(id) } }
                val patterns = async { patternIds.map { id -> loadPattern(id) } }
                UnitPreparation(vocabulary.await(), grammar.await(), patterns.await())
            }
        } else {
            UnitPreparation(emptyList(), emptyList(), emptyList())
        }
        return TrainingUnit(
            unitId = document.stringOrId(FirestoreField.UNIT_ID),
            scenarioId = document.requiredString(FirestoreField.SCENARIO_ID),
            title = document.requiredString(FirestoreField.TITLE),
            communicativeFocus = document.requiredString(FirestoreField.COMMUNICATIVE_FOCUS),
            preparation = loadedPreparation,
            requiredActions = document.requiredActions()
        )
    }

    private suspend fun loadVocabulary(id: String): VocabularyItem {
        val document = dataSource.getVocabulary(id)
        return contentMapper.toVocabulary(document, id)
    }

    private suspend fun loadGrammar(id: String): GrammarPoint {
        val document = dataSource.getGrammar(id)
        return contentMapper.toGrammar(document, id)
    }

    private suspend fun loadPattern(id: String): SentencePattern {
        val document = dataSource.getPattern(id)
        return contentMapper.toPattern(document, id)
    }
}
