package com.project.speakmate.ui.training.state

import androidx.compose.runtime.Immutable
import com.project.speakmate.domain.model.TrainingActivityScenario

@Immutable
data class GoalAssessmentUiState(
    val title: String = "",
    val description: String = "",
    val requiredActions: List<GoalAssessmentActionUiState> = emptyList(),
    val status: TrainingContentStatus = TrainingContentStatus.LOADING,
    val activity: TrainingActivityScenario? = null,
    val isReplay: Boolean = false,
    val isLocked: Boolean = false,
    val isStarting: Boolean = false,
    val isStartReady: Boolean = false,
    val activeCallActivity: TrainingActivityScenario? = null,
    val activeCallIsReplay: Boolean = false
)
