package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.utils.extension.defaultEmpty

@Immutable
data class TrainingPathUiState(
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val level: String = "",
    val goals: List<TrainingGoalUiState> = emptyList(),
    val canContinue: Boolean = false,
    val isUnassessed: Boolean = false,
    val isPathCompleted: Boolean = false,
    val activeDialog: TrainingPathDialog? = null,
    val title: String = "",
    val completedActivityCount: Int = 0,
    val totalActivityCount: Int = 0,
    val currentActivityTitle: String = "",
    val isAssessmentReady: Boolean = false,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val isOpeningActivity: Boolean = false,
    val failure: TrainingPathFailure? = null,
    val noticeDialog: TrainingPathNoticeUiState? = null,
    val pendingNavigation: TrainingActivityDestination? = null
) {
    val previewUnit: TrainingUnitUiState?
        get() = noticeDialog?.unit

    val notice: TrainingPathNotice?
        get() = noticeDialog?.notice

    val isLevelInformationVisible: Boolean
        get() = activeDialog == TrainingPathDialog.LevelInformation

    val selectedGoalId: String?
        get() = (activeDialog as? TrainingPathDialog.Goal)?.goalId

    val selectedGoal: GoalDetailUiState? =
        selectedGoalId?.let { id ->
            val goal = goals.firstOrNull { it.id == id }
            GoalDetailUiState(
                title = goal?.title.defaultEmpty(),
                description = goal?.summary.defaultEmpty(),
                units = goal?.units.defaultEmpty(),
                canPrepareCurrentUnit =
                    !isStale &&
                        !isRefreshing &&
                        !isOpeningActivity &&
                        goal?.units?.any { it.status == TrainingProgressStatus.CURRENT && it.canOpen } == true,
                isConfigured = goal?.isConfigured == true,
                isStale = isStale || isRefreshing,
                isLocked = goal?.status == TrainingProgressStatus.LOCKED
            )
        }
}
