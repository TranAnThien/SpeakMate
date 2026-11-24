package com.project.speakmate.data.remote.firebase

object FirestoreDocumentId {
    private const val PATH_SEPARATOR = '/'

    fun isValid(value: String): Boolean = value.isNotBlank() && PATH_SEPARATOR !in value
}
