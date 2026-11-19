package com.project.speakmate.data.remote.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import java.util.Date

object FirestoreStorageSize {
    private const val MAP_OVERHEAD_BYTES = 32L
    private const val DOCUMENT_NAME_OVERHEAD_BYTES = 16L
    private const val NUMBER_BYTES = 8L
    private const val GEO_POINT_BYTES = 16L
    private const val MAX_DEPTH = 20
    private const val PATH_SEPARATOR = "/"

    fun getDocumentBytes(path: String, fields: Map<String, Any?>): Long = getNameBytes(path) + getValueBytes(fields)

    fun getValueBytes(value: Any?): Long = getValueBytes(value, 0)

    private fun getValueBytes(value: Any?, depth: Int): Long {
        require(depth <= MAX_DEPTH)
        return when (value) {
            null, is Boolean -> 1L
            is String -> getStringBytes(value)
            is Number, is Date, is Timestamp -> NUMBER_BYTES
            is ByteArray -> value.size.toLong()
            is Blob -> value.toBytes().size.toLong()
            is GeoPoint -> GEO_POINT_BYTES
            is DocumentReference -> getNameBytes(value.path)
            is List<*> -> value.sumOf { getValueBytes(it, depth + 1) }
            is Map<*, *> -> MAP_OVERHEAD_BYTES + value.entries.sumOf { (key, field) ->
                getStringBytes(checkNotNull(key as? String)) + getValueBytes(field, depth + 1)
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun getStringBytes(value: String): Long = value.toByteArray(Charsets.UTF_8).size.toLong() + 1L

    private fun getNameBytes(path: String): Long = path.split(PATH_SEPARATOR).sumOf(::getStringBytes) + DOCUMENT_NAME_OVERHEAD_BYTES
}
