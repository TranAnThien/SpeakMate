package com.project.speakmate.ui.training.state

sealed interface TrainingActivityDestination {
    data class Unit(val unitId: String) : TrainingActivityDestination

    data class GoalAssessment(val goalId: String) : TrainingActivityDestination
}
