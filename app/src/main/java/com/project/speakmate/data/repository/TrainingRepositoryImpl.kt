package com.project.speakmate.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.domain.model.GrammarPoint
import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SentencePattern
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingGoal
import com.project.speakmate.domain.model.TrainingPath
import com.project.speakmate.domain.model.TrainingUnit
import com.project.speakmate.domain.model.UnitPreparation
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.repository.TrainingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TrainingRepository {
    private val paths get() = firestore.collection("training_paths")
    private val goals get() = firestore.collection("training_goals")
    private val units get() = firestore.collection("training_units")
    private val assessments get() = firestore.collection("goal_assessments")
    private val scenarios get() = firestore.collection("scenarios")

    override suspend fun getTrainingPath(level: SpeakingLevel): Result<TrainingPath> = runCatching {
        require(level != SpeakingLevel.NOT_ASSESSED) { "A training path requires an assessed level." }
        val document = paths.document(level.name).get().await().required("Training path ${level.name}")
        val goalIds = document.requiredStringList("goals")
        TrainingPath(
            pathId = document.stringOrId("pathId"),
            targetLevel = document.requiredLevel("targetLevel"),
            title = document.requiredString("title"),
            goals = coroutineScope { goalIds.map { id -> async { loadGoal(id) } }.awaitAll() }
        )
    }

    override suspend fun getTrainingGoal(goalId: String): Result<TrainingGoal> = runCatching { loadGoal(goalId) }

    override suspend fun getTrainingUnit(unitId: String): Result<TrainingUnit> = runCatching { loadUnit(unitId) }

    override suspend fun getScenario(scenarioId: String): Result<Scenario> = runCatching {
        val document = scenarios.document(scenarioId).get().await().required("Scenario $scenarioId")
        Scenario(
            scenarioId = document.stringOrId("scenarioId"),
            name = document.requiredString("name"),
            description = document.requiredString("description"),
            promptTemplate = document.requiredString("promptTemplate")
        )
    }

    override suspend fun getScenarioImageUrl(scenarioId: String): Result<String> = runCatching {
        scenarios.document(scenarioId).get().await()
            .required("Scenario $scenarioId")
            .getString("imageUrl")
            .orEmpty()
    }

    private suspend fun loadGoal(goalId: String): TrainingGoal {
        val document = goals.document(goalId).get().await().required("Training goal $goalId")
        val unitIds = document.requiredStringList("units")
        return TrainingGoal(
            goalId = document.stringOrId("goalId"),
            title = document.requiredString("title"),
            canDoDescription = document.requiredString("canDoDescription"),
            goalAssessment = loadAssessment(document.requiredString("goalAssessment")),
            units = coroutineScope { unitIds.map { id -> async { loadUnit(id) } }.awaitAll() }
        )
    }

    private suspend fun loadAssessment(assessmentId: String): GoalAssessment {
        val document = assessments.document(assessmentId).get().await().required("Goal assessment $assessmentId")
        return GoalAssessment(
            scenarioId = document.requiredString("scenarioId"),
            title = document.requiredString("title"),
            requiredActions = document.requiredActions()
        )
    }

    private suspend fun loadUnit(unitId: String): TrainingUnit {
        val document = units.document(unitId).get().await().required("Training unit $unitId")
        val preparation = document.requiredMap("preparation")
        val vocabularyIds = preparation.requiredReferenceIds("vocabulary")
        val grammarIds = preparation.requiredReferenceIds("grammarPoints")
        val patternIds = preparation.requiredReferenceIds("sentencePatterns")
        val loadedPreparation = coroutineScope {
            val vocabulary = async { vocabularyIds.map { id -> loadVocabulary(id) } }
            val grammar = async { grammarIds.map { id -> loadGrammar(id) } }
            val patterns = async { patternIds.map { id -> loadPattern(id) } }
            UnitPreparation(vocabulary.await(), grammar.await(), patterns.await())
        }
        return TrainingUnit(
            unitId = document.stringOrId("unitId"),
            scenarioId = document.requiredString("scenarioId"),
            title = document.requiredString("title"),
            communicativeFocus = document.requiredString("communicativeFocus"),
            preparation = loadedPreparation,
            requiredActions = document.requiredActions()
        )
    }

    private suspend fun loadVocabulary(id: String): VocabularyItem {
        val document = firestore.collection("vocabularies").document(id).get().await().required("Vocabulary $id")
        return VocabularyItem(
            word = document.requiredString("word"),
            mean = document.requiredString("mean"),
            phonetic = document.requiredString("phonetic"),
            partOfSpeech = document.requiredString("partOfSpeech"),
            example = document.requiredMap("example").toExample("Vocabulary $id example")
        )
    }

    private suspend fun loadGrammar(id: String): GrammarPoint {
        val document = firestore.collection("grammar_points").document(id).get().await().required("Grammar point $id")
        return GrammarPoint(
            title = document.requiredString("title"),
            structure = document.requiredString("structure"),
            explanation = document.requiredString("explanation"),
            usage = document.requiredString("usage"),
            examples = document.requiredExampleList("examples", "Grammar point $id")
        )
    }

    private suspend fun loadPattern(id: String): SentencePattern {
        val document = firestore.collection("sentence_patterns").document(id).get().await().required("Sentence pattern $id")
        return SentencePattern(
            pattern = document.requiredString("pattern"),
            mean = document.requiredString("mean"),
            usage = document.requiredString("usage"),
            examples = document.requiredExampleList("examples", "Sentence pattern $id")
        )
    }
}

private fun DocumentSnapshot.required(label: String): DocumentSnapshot =
    takeIf { exists() } ?: error("$label is missing from Firestore.")

private fun DocumentSnapshot.stringOrId(field: String): String = getString(field)?.takeIf(String::isNotBlank) ?: id

private fun DocumentSnapshot.requiredString(field: String): String =
    getString(field)?.takeIf(String::isNotBlank) ?: error("Document '$id' is missing '$field'.")

private fun DocumentSnapshot.requiredLevel(field: String): SpeakingLevel =
    runCatching { SpeakingLevel.valueOf(requiredString(field)) }.getOrElse {
        error("Document '$id' has an invalid '$field'.")
    }

private fun DocumentSnapshot.requiredStringList(field: String): List<String> =
    (get(field) as? List<*>)?.mapIndexed { index, value ->
        value as? String ?: error("Document '$id' has an invalid '$field[$index]'.")
    }?.takeIf(List<String>::isNotEmpty) ?: error("Document '$id' is missing '$field'.")

private fun DocumentSnapshot.requiredMap(field: String): Map<*, *> =
    get(field) as? Map<*, *> ?: error("Document '$id' is missing '$field'.")

private fun DocumentSnapshot.requiredActions(): List<RequiredAction> =
    (get("requiredActions") as? List<*>)?.mapIndexed { index, value ->
        (value as? Map<*, *>)?.toRequiredAction("Document '$id' requiredActions[$index]")
            ?: error("Document '$id' has an invalid requiredActions[$index].")
    } ?: error("Document '$id' is missing 'requiredActions'.")

private fun Map<*, *>.requiredReferenceIds(field: String): List<String> =
    (this[field] as? List<*>)?.mapIndexed { index, value ->
        val reference = value as? Map<*, *> ?: error("Preparation has an invalid '$field[$index]'.")
        reference["id"] as? String ?: error("Preparation '$field[$index]' is missing its id.")
    } ?: error("Preparation is missing '$field'.")

private fun DocumentSnapshot.requiredExampleList(field: String, label: String): List<LearningExample> =
    (get(field) as? List<*>)?.mapIndexed { index, value ->
        (value as? Map<*, *>)?.toExample("$label $field[$index]")
            ?: error("$label has an invalid '$field[$index]'.")
    } ?: error("$label is missing '$field'.")

private fun Map<*, *>.toExample(label: String): LearningExample = LearningExample(
    sentence = requiredText("sentence", label),
    translate = requiredText("translate", label)
)

private fun Map<*, *>.toRequiredAction(label: String): RequiredAction = RequiredAction(
    requiredActionId = requiredText("requiredActionId", label),
    description = requiredText("description", label)
)

private fun Map<*, *>.requiredText(field: String, label: String): String =
    this[field] as? String ?: error("$label is missing '$field'.")
