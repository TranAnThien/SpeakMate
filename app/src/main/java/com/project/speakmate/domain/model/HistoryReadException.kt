package com.project.speakmate.domain.model

class HistoryReadException(val reason: HistoryReadFailure, cause: Throwable? = null) : Exception(cause)
