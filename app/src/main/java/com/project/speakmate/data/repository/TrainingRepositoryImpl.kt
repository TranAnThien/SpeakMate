package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.required
import com.project.speakmate.data.remote.firebase.requiredActions
import com.project.speakmate.data.remote.firebase.requiredExampleList
import com.project.speakmate.data.remote.firebase.requiredLevel
import com.project.speakmate.data.remote.firebase.requiredMap
import com.project.speakmate.data.remote.firebase.requiredReferenceIds
import com.project.speakmate.data.remote.firebase.requiredString
import com.project.speakmate.data.remote.firebase.requiredStringList
import com.project.speakmate.data.remote.firebase.stringOrId
import com.project.speakmate.data.remote.firebase.toExample
import com.project.speakmate.data.remote.firebase.FirestoreCollection
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.utils.extension.suspendRunCatching

import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.domain.model.GrammarPoint
import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SentencePattern
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.ScenarioRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val scenarioRepository: ScenarioRepository
) : TrainingRepository {
    private val paths get() = firestore.collection(FirestoreCollection.TRAINING_PATHS)
    private val goals get() = firestore.collection(FirestoreCollection.TRAINING_GOALS)
    private val units get() = firestore.collection(FirestoreCollection.TRAINING_UNITS)
    private val assessments get() = firestore.collection(FirestoreCollection.GOAL_ASSESSMENTS)

    override suspend fun getTrainingPath(level: SpeakingLevel): Result<TrainingPath> = suspendRunCatching {
        require(level != SpeakingLevel.NOT_ASSESSED) { "A training path requires an assessed level." }
        val document = paths.document(level.name).get().await().required("Training path ${level.name}")
        val goalIds = document.requiredStringList(FirestoreField.GOALS)
        TrainingPath(
            pathId = document.stringOrId(FirestoreField.PATH_ID),
            targetLevel = document.requiredLevel(FirestoreField.TARGET_LEVEL),
            title = document.requiredString(FirestoreField.TITLE),
            goals = coroutineScope { goalIds.map { id -> async { loadGoal(id) } }.awaitAll() }
        )
    }

    override suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal> = suspendRunCatching { loadGoal(goalId) }

    override suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit> = suspendRunCatching { loadUnit(unitId) }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = suspendRunCatching {
        checkNotNull(scenarioRepository.getScenario(scenarioId)) { "Scenario '$scenarioId' is missing or invalid." }
    }

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> =
        suspendRunCatching { scenarioRepository.getScenarioImageUrl(scenarioId) }

    private suspend fun loadGoal(goalId: String): TrainingGoal {
        val document = goals.document(goalId).get().await().required("Training goal $goalId")
        val unitIds = document.requiredStringList(FirestoreField.UNITS)
        return TrainingGoal(
            goalId = document.stringOrId(FirestoreField.GOAL_ID),
            title = document.requiredString(FirestoreField.TITLE),
            canDoDescription = document.requiredString(FirestoreField.CAN_DO_DESCRIPTION),
            goalAssessment = loadAssessment(document.requiredString(FirestoreField.GOAL_ASSESSMENT)),
            units = coroutineScope { unitIds.map { id -> async { loadUnit(id) } }.awaitAll() }
        )
    }

    private suspend fun loadAssessment(assessmentId: String): GoalAssessment {
        val document = assessments.document(assessmentId).get().await().required("Goal assessment $assessmentId")
        return GoalAssessment(
            scenarioId = document.requiredString(FirestoreField.SCENARIO_ID),
            title = document.requiredString(FirestoreField.TITLE),
            requiredActions = document.requiredActions()
        )
    }

    private suspend fun loadUnit(unitId: String): TrainingUnit {
        val document = units.document(unitId).get().await().required("Training unit $unitId")
        val preparation = document.requiredMap(FirestoreField.PREPARATION)
        val vocabularyIds = preparation.requiredReferenceIds(FirestoreField.VOCABULARY)
        val grammarIds = preparation.requiredReferenceIds(FirestoreField.GRAMMAR_POINTS)
        val patternIds = preparation.requiredReferenceIds(FirestoreField.SENTENCE_PATTERNS)
        val loadedPreparation = coroutineScope {
            val vocabulary = async { vocabularyIds.map { id -> loadVocabulary(id) } }
            val grammar = async { grammarIds.map { id -> loadGrammar(id) } }
            val patterns = async { patternIds.map { id -> loadPattern(id) } }
            UnitPreparation(vocabulary.await(), grammar.await(), patterns.await())
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
        val document = firestore.collection(FirestoreCollection.VOCABULARIES).document(id).get().await().required("Vocabulary $id")
        return VocabularyItem(
            word = document.requiredString(FirestoreField.WORD),
            mean = document.requiredString(FirestoreField.MEAN),
            phonetic = document.requiredString(FirestoreField.PHONETIC),
            partOfSpeech = document.requiredString(FirestoreField.PART_OF_SPEECH),
            example = document.requiredMap(FirestoreField.EXAMPLE).toExample("Vocabulary $id example")
        )
    }

    private suspend fun loadGrammar(id: String): GrammarPoint {
        val document = firestore.collection(FirestoreCollection.GRAMMAR_POINTS).document(id).get().await().required("Grammar point $id")
        return GrammarPoint(
            title = document.requiredString(FirestoreField.TITLE),
            structure = document.requiredString(FirestoreField.STRUCTURE),
            explanation = document.requiredString(FirestoreField.EXPLANATION),
            usage = document.requiredString(FirestoreField.USAGE),
            examples = document.requiredExampleList(FirestoreField.EXAMPLES, "Grammar point $id")
        )
    }

    private suspend fun loadPattern(id: String): SentencePattern {
        val document = firestore.collection(FirestoreCollection.SENTENCE_PATTERNS).document(id).get().await().required("Sentence pattern $id")
        return SentencePattern(
            pattern = document.requiredString(FirestoreField.PATTERN),
            mean = document.requiredString(FirestoreField.MEAN),
            usage = document.requiredString(FirestoreField.USAGE),
            examples = document.requiredExampleList(FirestoreField.EXAMPLES, "Sentence pattern $id")
        )
    }
}
