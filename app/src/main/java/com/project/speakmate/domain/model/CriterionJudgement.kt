package com.project.speakmate.domain.model

data class CriterionJudgement(
    val isMet: Boolean,
    val userQuote: String,
    val feedback: String,
    val audioStartMillis: Long?,
    val audioEndMillis: Long?
)
