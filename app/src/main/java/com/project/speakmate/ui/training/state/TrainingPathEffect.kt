package com.project.speakmate.ui.training.state

sealed interface TrainingPathEffect {
    data class OpenActivity(val destination: TrainingActivityDestination) : TrainingPathEffect
}
