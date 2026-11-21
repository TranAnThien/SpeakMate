package com.project.speakmate.data.remote.ai

import com.project.speakmate.domain.model.CallFailure
import com.project.speakmate.utils.AppConstant

class GeminiLiveFailureMapper {
    companion object {
        private const val ACCESS_DENIED = 403
        private const val UNAUTHORIZED = 401
        private const val NOT_FOUND = 404
    }

    fun getFailure(reason: String, status: Int? = null): CallFailure {
        val detail = reason.lowercase()
        return when {
            status == AppConstant.TOO_MANY_REQUESTS || "resource_exhausted" in detail || "resource has been exhausted" in detail ||
                "quota" in detail -> CallFailure.QUOTA
            status == UNAUTHORIZED || status == ACCESS_DENIED || "api key" in detail ||
                "permission_denied" in detail || "unauthenticated" in detail -> CallFailure.ACCESS
            status == NOT_FOUND || "model" in detail &&
                ("not found" in detail || "not supported" in detail || "not available" in detail) -> CallFailure.MODEL
            else -> CallFailure.NETWORK
        }
    }
}
