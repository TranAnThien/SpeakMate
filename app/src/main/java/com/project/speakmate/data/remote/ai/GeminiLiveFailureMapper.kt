package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.utils.AppConstant
import java.util.Locale

class GeminiLiveFailureMapper {
    companion object {
        private const val RESOURCE_EXHAUSTED_REASON = "resource_exhausted"
        private const val EXHAUSTED_RESOURCE_REASON = "resource has been exhausted"
        private const val QUOTA_REASON = "quota"
        private const val API_KEY_REASON = "api key"
        private const val PERMISSION_DENIED_REASON = "permission_denied"
        private const val UNAUTHENTICATED_REASON = "unauthenticated"
        private const val MODEL_REASON = "model"
        private const val MODEL_NOT_FOUND_REASON = "not found"
        private const val MODEL_NOT_SUPPORTED_REASON = "not supported"
        private const val MODEL_NOT_AVAILABLE_REASON = "not available"
        private const val ACCESS_DENIED = 403
        private const val UNAUTHORIZED = 401
        private const val NOT_FOUND = 404
    }

    fun getFailure(reason: String, status: Int? = null): CallFailure {
        val detail = reason.lowercase(Locale.ROOT)
        return when {
            status == AppConstant.TOO_MANY_REQUESTS ||
                RESOURCE_EXHAUSTED_REASON in detail ||
                EXHAUSTED_RESOURCE_REASON in detail ||
                QUOTA_REASON in detail -> CallFailure.QUOTA
            status == UNAUTHORIZED ||
                status == ACCESS_DENIED ||
                API_KEY_REASON in detail ||
                PERMISSION_DENIED_REASON in detail ||
                UNAUTHENTICATED_REASON in detail -> CallFailure.ACCESS
            status == NOT_FOUND ||
                MODEL_REASON in detail &&
                    (MODEL_NOT_FOUND_REASON in detail ||
                        MODEL_NOT_SUPPORTED_REASON in detail ||
                        MODEL_NOT_AVAILABLE_REASON in detail) -> CallFailure.MODEL
            else -> CallFailure.NETWORK
        }
    }
}
