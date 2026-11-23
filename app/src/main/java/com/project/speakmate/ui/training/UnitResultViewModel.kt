package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.training.state.UnitResultActionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.ui.training.state.UnitResultPresentation
import com.project.speakmate.ui.training.state.UnitResultUiState
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.defaultFalse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UnitResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val completionRepository: PracticeCompletionRepository
) : ViewModel() {
    private val route = try { savedStateHandle.toRoute<AppRoute.UnitResult>() } catch (_: IllegalArgumentException) { null }
    private val attemptId = route?.attemptId.defaultEmpty()
    private val _uiState = MutableStateFlow(UnitResultUiState())
    val uiState: StateFlow<UnitResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                completionRepository.observeCompletion(attemptId).collect { state ->
                    _uiState.update {
                        when (state) {
                            PracticeCompletionState.Grading -> UnitResultUiState()
                            is PracticeCompletionState.AwaitingGrading -> UnitResultUiState(isAwaitingGrading = true)
                            is PracticeCompletionState.Saving -> mapCompletion(state.completion).copy(isSaving = true)
                            PracticeCompletionState.Unavailable -> UnitResultUiState(
                                presentation = UnitResultPresentation.INSUFFICIENT_EVIDENCE, isUnavailable = true
                            )
                            is PracticeCompletionState.Saved -> mapCompletion(state.completion).copy(isSaved = true)
                            is PracticeCompletionState.SaveFailed -> mapCompletion(state.completion).copy(saveFailed = true, isSaving = state.isSaving, saveFailure = state.reason)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { UnitResultUiState(presentation = UnitResultPresentation.INSUFFICIENT_EVIDENCE, isUnavailable = true) }
            }
        }
    }

    override fun onCleared() {
        completionRepository.releaseCompletion(attemptId)
        super.onCleared()
    }

    fun retryGrading() { completionRepository.retryGrading(attemptId) }

    fun saveUnassessed() { completionRepository.saveUnassessed(attemptId) }

    fun retrySave() {
        completionRepository.retrySave(attemptId)
    }

    private fun mapCompletion(completion: PracticeCompletion): UnitResultUiState {
        val attempt = completion.attempt
        val evaluated = completion.outcome as? AssessmentOutcome.Evaluated
        val presentation = when (attempt.result) {
            AssessmentResult.PASS -> UnitResultPresentation.PASS
            AssessmentResult.PARTIAL -> UnitResultPresentation.PARTIAL
            AssessmentResult.FAIL -> if (attempt.requiredActionResults.containsValue(false)) UnitResultPresentation.FAIL_REQUIRED_ACTION else UnitResultPresentation.FAIL_CRITERIA
            null -> UnitResultPresentation.INSUFFICIENT_EVIDENCE
        }
        return UnitResultUiState(
            presentation = presentation,
            isReplay = route?.isReplay.defaultFalse(),
            insufficientReason = (completion.outcome as? AssessmentOutcome.InsufficientEvidence)?.reason,
            title = completion.training?.scenario?.name.defaultEmpty(),
            feedback = attempt.feedbackSummary.defaultEmpty(),
            requiredActions = completion.training?.requiredActions.defaultEmpty().map { action ->
                UnitResultActionUiState(action.requiredActionId, action.description, evaluation(attempt.requiredActionResults[action.requiredActionId]))
            },
            criteria = TrainingResultMapper.toCriteria(evaluated)
        )
    }

    private fun evaluation(isMet: Boolean?): UnitResultEvaluation = when (isMet) {
        true -> UnitResultEvaluation.PASSED
        false -> UnitResultEvaluation.NOT_PASSED
        null -> UnitResultEvaluation.NOT_EVALUATED
    }
}
