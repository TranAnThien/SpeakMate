package com.project.speakmate.domain.model

enum class CallActivityType(val isTranscriptAvailable: Boolean) {
    FREE_PRACTICE(true),
    TRAINING_UNIT(true),
    GOAL_ASSESSMENT(false),
    INITIAL_PLACEMENT(false),
    REASSESSMENT(false)
}
