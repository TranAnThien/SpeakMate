package com.project.speakmate.domain.model

class PracticeSaveException(val reason: PracticeSaveFailure, cause: Throwable? = null) : Exception(reason.name, cause)
