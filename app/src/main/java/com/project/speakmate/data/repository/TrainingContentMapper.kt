package com.project.speakmate.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.project.speakmate.data.remote.firebase.FirestoreField
import com.project.speakmate.data.remote.firebase.requiredActions
import com.project.speakmate.data.remote.firebase.requiredExampleList
import com.project.speakmate.data.remote.firebase.requiredMap
import com.project.speakmate.data.remote.firebase.requiredString
import com.project.speakmate.data.remote.firebase.toExample
import com.project.speakmate.domain.model.GoalAssessment
import com.project.speakmate.domain.model.GrammarPoint
import com.project.speakmate.domain.model.SentencePattern
import com.project.speakmate.domain.model.VocabularyItem

class TrainingContentMapper {
    fun toAssessment(document: DocumentSnapshot): GoalAssessment {
        return GoalAssessment(
            scenarioId = document.requiredString(FirestoreField.SCENARIO_ID),
            title = document.requiredString(FirestoreField.TITLE),
            requiredActions = document.requiredActions()
        )
    }

    fun toVocabulary(document: DocumentSnapshot, id: String): VocabularyItem {
        return VocabularyItem(
            word = document.requiredString(FirestoreField.WORD),
            mean = document.requiredString(FirestoreField.MEAN),
            phonetic = document.requiredString(FirestoreField.PHONETIC),
            partOfSpeech = document.requiredString(FirestoreField.PART_OF_SPEECH),
            example = document.requiredMap(FirestoreField.EXAMPLE).toExample("Vocabulary $id example")
        )
    }

    fun toGrammar(document: DocumentSnapshot, id: String): GrammarPoint {
        return GrammarPoint(
            title = document.requiredString(FirestoreField.TITLE),
            structure = document.requiredString(FirestoreField.STRUCTURE),
            explanation = document.requiredString(FirestoreField.EXPLANATION),
            usage = document.requiredString(FirestoreField.USAGE),
            examples = document.requiredExampleList(FirestoreField.EXAMPLES, "Grammar point $id")
        )
    }

    fun toPattern(document: DocumentSnapshot, id: String): SentencePattern {
        return SentencePattern(
            pattern = document.requiredString(FirestoreField.PATTERN),
            mean = document.requiredString(FirestoreField.MEAN),
            usage = document.requiredString(FirestoreField.USAGE),
            examples = document.requiredExampleList(FirestoreField.EXAMPLES, "Sentence pattern $id")
        )
    }

}
