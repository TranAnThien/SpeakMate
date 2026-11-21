package com.project.speakmate.app

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Auth : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object TrainingPath : AppRoute

    @Serializable
    data object ScenarioCatalog : AppRoute

    @Serializable
    data object History : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object SavedVocabulary : AppRoute

    @Serializable
    data object Placement : AppRoute

    @Serializable
    data class PlacementStep(val isCalling: Boolean = false) : AppRoute

    @Serializable
    data object Reassessment : AppRoute

    @Serializable
    data class ReassessmentStep(val isCalling: Boolean = false) : AppRoute

    @Serializable
    data object PathCompletion : AppRoute

    @Serializable
    data class GoalDetail(val goalId: String) : AppRoute

    @Serializable
    data class UnitPreparation(val unitId: String) : AppRoute

    @Serializable
    data class UnitCall(val unitId: String, val isRetry: Boolean = false) : AppRoute

    @Serializable
    data class UnitResult(val unitId: String, val attemptId: String) : AppRoute

    @Serializable
    data class GoalAssessment(
        val goalId: String,
        val isCalling: Boolean = false,
        val isRetry: Boolean = false
    ) : AppRoute

    @Serializable
    data class GoalAssessmentResult(
        val goalId: String,
        val attemptId: String,
        val isReplay: Boolean = false
    ) : AppRoute

    @Serializable
    data class ScenarioPreparation(val practiceId: String, val isCalling: Boolean = false) : AppRoute

    @Serializable
    data class SessionDetail(val attemptId: String) : AppRoute
}
