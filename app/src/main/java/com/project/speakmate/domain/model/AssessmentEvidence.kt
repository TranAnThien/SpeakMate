package com.project.speakmate.domain.model

data class AssessmentEvidence(
    val targetLevel: SpeakingLevel,
    val communicativeGoal: String,
    val requiredActions: List<RequiredAction>,
    val callOutcome: CallOutcome,
    val failure: CallFailure?,
    val hasUserSpeech: Boolean,
    val transcript: List<TranscriptMessage>,
    val audioDurationMillis: Long?,
    val assessmentAudioId: String? = null,
    val operationId: String? = null,
    val taskId: String? = null,
    val expiresAtMillis: Long? = null,
    val userId: String? = null
)
