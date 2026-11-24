package com.project.speakmate.ui.training.state

sealed interface TrainingPathDialog {
    data object LevelInformation : TrainingPathDialog

    data class Goal(val goalId: String) : TrainingPathDialog
}
