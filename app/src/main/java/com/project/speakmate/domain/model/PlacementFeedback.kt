package com.project.speakmate.domain.model

data class PlacementFeedback(
    val range: String = "",
    val accuracy: String = "",
    val fluency: String = "",
    val interaction: String = "",
    val coherence: String = ""
)
