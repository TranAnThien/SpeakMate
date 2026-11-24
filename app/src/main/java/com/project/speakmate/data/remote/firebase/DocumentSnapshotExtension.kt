package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.utils.extension.default

fun DocumentSnapshot.required(label: String): DocumentSnapshot =
    takeIf { exists() } ?: error("$label is missing from Firestore.")

fun DocumentSnapshot.stringOrId(field: String): String = getString(field)?.takeIf(String::isNotBlank).default(id)

fun DocumentSnapshot.requiredString(field: String): String =
    getString(field)?.takeIf(String::isNotBlank) ?: error("Document '$id' is missing '$field'.")

fun DocumentSnapshot.requiredLevel(field: String): SpeakingLevel =
    runCatching { SpeakingLevel.valueOf(requiredString(field)) }.getOrElse {
        error("Document '$id' has an invalid '$field'.")
    }

fun DocumentSnapshot.requiredStringList(field: String): List<String> =
    (get(field) as? List<*>)?.mapIndexed { index, value ->
        value as? String ?: error("Document '$id' has an invalid '$field[$index]'.")
    }?.takeIf(List<String>::isNotEmpty) ?: error("Document '$id' is missing '$field'.")

fun DocumentSnapshot.requiredMap(field: String): Map<*, *> =
    get(field) as? Map<*, *> ?: error("Document '$id' is missing '$field'.")

fun DocumentSnapshot.requiredActions(): List<RequiredAction> =
    (get(FirestoreField.REQUIRED_ACTIONS) as? List<*>)?.mapIndexed { index, value ->
        (value as? Map<*, *>)?.toRequiredAction("Document '$id' requiredActions[$index]")
            ?: error("Document '$id' has an invalid requiredActions[$index].")
    } ?: error("Document '$id' is missing 'requiredActions'.")

fun DocumentSnapshot.requiredExampleList(field: String, label: String): List<LearningExample> =
    (get(field) as? List<*>)?.mapIndexed { index, value ->
        (value as? Map<*, *>)?.toExample("$label $field[$index]")
            ?: error("$label has an invalid '$field[$index]'.")
    } ?: error("$label is missing '$field'.")

fun Map<*, *>.toExample(label: String): LearningExample = LearningExample(
    sentence = requiredText(FirestoreField.SENTENCE, label),
    translate = requiredText(FirestoreField.TRANSLATE, label)
)

private fun Map<*, *>.toRequiredAction(label: String): RequiredAction = RequiredAction(
    requiredActionId = requiredText(FirestoreField.REQUIRED_ACTION_ID, label),
    description = requiredText(FirestoreField.DESCRIPTION, label)
)

private fun Map<*, *>.requiredText(field: String, label: String): String =
    this[field] as? String ?: error("$label is missing '$field'.")
