package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiLiveFailureMapperTest {
    private val mapper = GeminiLiveFailureMapper()

    @Test
    fun httpStatusMapsQuotaAccessAndMissingModel() {
        mapOf(429 to CallFailure.QUOTA, 401 to CallFailure.ACCESS, 403 to CallFailure.ACCESS, 404 to CallFailure.MODEL).forEach { (status, failure) ->
            assertEquals(failure, mapper.getFailure("", status))
        }
    }

    @Test
    fun quotaReasonsAreCaseInsensitive() {
        listOf("RESOURCE_EXHAUSTED", "Resource has been exhausted", "QUOTA exceeded").forEach {
            assertEquals(CallFailure.QUOTA, mapper.getFailure(it))
        }
    }

    @Test
    fun accessReasonsAreRecognizedWithoutHttpStatus() {
        listOf("API KEY invalid", "PERMISSION_DENIED", "UNAUTHENTICATED").forEach {
            assertEquals(CallFailure.ACCESS, mapper.getFailure(it))
        }
    }

    @Test
    fun modelReasonRequiresBothModelAndAvailabilityFailure() {
        listOf("MODEL not found", "model not supported", "model not available").forEach {
            assertEquals(CallFailure.MODEL, mapper.getFailure(it))
        }
        assertEquals(CallFailure.NETWORK, mapper.getFailure("model response failed"))
        assertEquals(CallFailure.NETWORK, mapper.getFailure("resource not found"))
    }

    @Test
    fun unknownAndTransientStatusesUseNetworkFailure() {
        listOf(null, 400, 500, 503).forEach { assertEquals(CallFailure.NETWORK, mapper.getFailure("", it)) }
    }

    @Test
    fun quotaClassificationPrecedesConflictingAccessAndModelSignals() {
        assertEquals(CallFailure.QUOTA, mapper.getFailure("quota", 401))
        assertEquals(CallFailure.ACCESS, mapper.getFailure("api key", 404))
    }
}
