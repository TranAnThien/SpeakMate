package com.project.speakmate.domain.model

sealed interface CallActivityContext {
    val scenario: Scenario
    val activityType: CallActivityType
    val communicativeGoal: String

    data class FreePractice(override val scenario: Scenario) : CallActivityContext {
        override val activityType = CallActivityType.FREE_PRACTICE
        override val communicativeGoal = ""
    }

    data class Training(val activityScenario: TrainingActivityScenario) : CallActivityContext {
        override val scenario get() = activityScenario.scenario
        override val activityType get() = when (activityScenario.activity) {
            is TrainingActivity.UnitActivity -> CallActivityType.TRAINING_UNIT
            is TrainingActivity.GoalAssessmentActivity -> CallActivityType.GOAL_ASSESSMENT
        }
        override val communicativeGoal get() = activityScenario.communicativeGoal
    }

    data class Assessment(
        override val scenario: Scenario,
        override val activityType: CallActivityType,
        override val communicativeGoal: String,
        val step: Int? = null,
        val targetLevel: SpeakingLevel? = null,
        val role: PlacementRole? = null
    ) : CallActivityContext {
        init {
            require(activityType == CallActivityType.INITIAL_PLACEMENT || activityType == CallActivityType.REASSESSMENT)
        }
    }
}
