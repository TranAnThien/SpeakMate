package com.project.speakmate.ui.training

import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.TrainingProgressRules
import com.project.speakmate.ui.training.state.GoalAssessmentPresentation
import com.project.speakmate.ui.training.state.GoalAssessmentResultUiState
import com.project.speakmate.utils.extension.defaultFalse
import com.project.speakmate.utils.extension.defaultEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GoalAssessmentResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val completionRepository: PracticeCompletionRepository,
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val route = try { savedStateHandle.toRoute<AppRoute.GoalAssessmentResult>() } catch (_: IllegalArgumentException) { null }
    private val attemptId = route?.attemptId.defaultEmpty()
    private val goalId = route?.goalId.defaultEmpty()
    private val isReplay = route?.isReplay.defaultFalse()
    private val _uiState = MutableStateFlow(GoalAssessmentResultUiState())
    val uiState: StateFlow<GoalAssessmentResultUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeResult()
    }

    override fun onCleared() {
        completionRepository.releaseCompletion(attemptId)
        super.onCleared()
    }

    fun retrySave() {
        if (_uiState.value.isSaveFailed && !_uiState.value.isSaving) completionRepository.retrySave(attemptId)
    }

    fun retry() {
        observeResult()
    }

    private fun observeResult() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    if (uid == null) {
                        _uiState.update { GoalAssessmentResultUiState(isUnavailable = true) }
                        return@collectLatest
                    }
                    completionRepository.observeCompletion(attemptId).collectLatest { state ->
                        when (state) {
                            PracticeCompletionState.Grading -> _uiState.update { GoalAssessmentResultUiState() }
                            PracticeCompletionState.Unavailable -> _uiState.update { GoalAssessmentResultUiState(isUnavailable = true) }
                            is PracticeCompletionState.SaveFailed -> _uiState.update {
                                mapCompletion(state.completion).copy(isSaveFailed = true, isSaving = state.isSaving)
                            }
                            is PracticeCompletionState.Saved -> {
                                val completion = state.completion
                                val result = mapCompletion(completion).copy(isSaved = true)
                                _uiState.update {
                                    if (completion.attempt.result == AssessmentResult.PASS && !isReplay) {
                                        result.copy(presentation = GoalAssessmentPresentation.PROCESSING)
                                    } else result
                                }
                                if (completion.attempt.result == AssessmentResult.PASS && !isReplay) {
                                    try {
                                        val training = checkNotNull(completion.training)
                                        val path = trainingRepository.getTrainingPath(training.targetLevel).getOrThrow()
                                        val activity = checkNotNull(training.activity as? TrainingActivity.GoalAssessmentActivity)
                                        check(path.goals.getOrNull(activity.goalIndex)?.goalId == goalId)
                                        val user = checkNotNull(userRepository.getUser(uid).getOrThrow())
                                        val progress = checkNotNull(user.trainingProgress)
                                        if (user.level == training.targetLevel) {
                                            check(TrainingProgressRules.validate(path, progress) == null)
                                            _uiState.update { result }
                                            when {
                                                progress.stage == TrainingStage.PATH_COMPLETED && activity.goalIndex == path.goals.lastIndex -> _uiState.update {
                                                    result.copy(presentation = GoalAssessmentPresentation.PATH_COMPLETED)
                                                }
                                                progress.currentGoalOrder > activity.goalIndex -> _uiState.update {
                                                    result.copy(
                                                        presentation = GoalAssessmentPresentation.NEXT_GOAL,
                                                        nextGoalId = path.goals.getOrNull(activity.goalIndex + 1)?.goalId
                                                    )
                                                }
                                            }
                                        } else {
                                            _uiState.update { result }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        _uiState.update { result.copy(hasLoadError = true) }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { GoalAssessmentResultUiState(hasLoadError = true) }
            }
        }
    }

    private fun mapCompletion(completion: PracticeCompletion): GoalAssessmentResultUiState {
        check(completion.training?.activity is TrainingActivity.GoalAssessmentActivity)
        return GoalAssessmentResultUiState(
            presentation = when (completion.attempt.result) {
                AssessmentResult.PASS -> GoalAssessmentPresentation.REPLAY_COMPLETED
                AssessmentResult.PARTIAL -> GoalAssessmentPresentation.PARTIAL
                AssessmentResult.FAIL -> if (completion.attempt.requiredActionResults.containsValue(false)) {
                    GoalAssessmentPresentation.FAIL_REQUIRED_ACTION
                } else GoalAssessmentPresentation.FAIL_CRITERIA
                null -> GoalAssessmentPresentation.INSUFFICIENT_EVIDENCE
            },
            feedback = completion.attempt.feedbackSummary.defaultEmpty(),
            level = checkNotNull(completion.training).targetLevel
        )
    }
}
