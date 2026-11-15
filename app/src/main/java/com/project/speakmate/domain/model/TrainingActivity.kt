package com.project.speakmate.domain.model

sealed interface TrainingActivity {
    val goalIndex: Int

    data class UnitActivity(override val goalIndex: Int, val unitIndex: Int) : TrainingActivity

    data class GoalAssessmentActivity(override val goalIndex: Int) : TrainingActivity
}
