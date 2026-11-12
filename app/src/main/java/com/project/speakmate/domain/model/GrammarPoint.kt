package com.project.speakmate.domain.model

data class GrammarPoint(
    val title: String,
    val structure: String,
    val explanation: String,
    val usage: String,
    val examples: List<LearningExample>
)