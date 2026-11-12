package com.project.speakmate.domain.model

data class SentencePattern(
    val pattern: String,
    val mean: String,
    val usage: String,
    val examples: List<LearningExample>
)