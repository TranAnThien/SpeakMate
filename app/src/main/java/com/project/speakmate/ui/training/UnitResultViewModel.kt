package com.project.speakmate.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.PracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.training.state.UnitResultActionUiState
import com.project.speakmate.ui.training.state.UnitResultCriterionType
import com.project.speakmate.ui.training.state.UnitResultCriterionUiState
import com.project.speakmate.ui.training.state.UnitResultEvaluation
import com.project.speakmate.ui.training.state.UnitResultPresentation
import com.project.speakmate.ui.training.state.UnitResultUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.defaultEmpty
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
    private val attemptId = savedStateHandle.get<String>(AppConstant.ATTEMPT_ID_ARGUMENT).defaultEmpty()
    private val _uiState = MutableStateFlow(UnitResultUiState())
    val uiState: StateFlow<UnitResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                completionRepository.observeCompletion(attemptId).collect { state ->
                    _uiState.update {
                        when (state) {
                            PracticeCompletionState.Grading -> UnitResultUiState()
                            PracticeCompletionState.Unavailable -> UnitResultUiState(
                                presentation = UnitResultPresentation.INSUFFICIENT_EVIDENCE, isUnavailable = true
                            )
                            is PracticeCompletionState.Saved -> mapCompletion(state.completion)
                            is PracticeCompletionState.SaveFailed -> mapCompletion(state.completion).copy(saveFailed = true, isSaving = state.isSaving)
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

    fun retrySave() {
        completionRepository.retrySave(attemptId)
    }

    private fun mapCompletion(completion: PracticeCompletion): UnitResultUiState {
        val attempt = completion.attempt
        val evaluated = completion.outcome as? AssessmentOutcome.Evaluated
        val criteria = evaluated?.criteria
        val judgement = evaluated?.evaluation
        val values = listOf(criteria?.rangeMet, criteria?.accuracyMet, criteria?.fluencyMet, criteria?.interactionMet, criteria?.coherenceMet)
        val feedback = listOf(judgement?.range?.feedback, judgement?.accuracy?.feedback, judgement?.fluency?.feedback, judgement?.interaction?.feedback, judgement?.coherence?.feedback)
        val presentation = when (attempt.result) {
            AssessmentResult.PASS -> UnitResultPresentation.PASS
            AssessmentResult.PARTIAL -> UnitResultPresentation.PARTIAL
            AssessmentResult.FAIL -> if (attempt.requiredActionResults.containsValue(false)) UnitResultPresentation.FAIL_REQUIRED_ACTION else UnitResultPresentation.FAIL_CRITERIA
            null -> UnitResultPresentation.INSUFFICIENT_EVIDENCE
        }
        return UnitResultUiState(
            presentation = presentation,
            title = completion.training?.scenario?.name.defaultEmpty(),
            feedback = attempt.feedbackSummary.defaultEmpty(),
            requiredActions = completion.training?.requiredActions.defaultEmpty().map { action ->
                UnitResultActionUiState(action.requiredActionId, action.description, evaluation(attempt.requiredActionResults[action.requiredActionId]))
            },
            criteria = UnitResultCriterionType.entries.mapIndexed { index, criterion ->
                UnitResultCriterionUiState(criterion.name, description = feedback[index].defaultEmpty(), evaluation = evaluation(values[index]), criterion = criterion)
            }
        )
    }

    private fun evaluation(isMet: Boolean?): UnitResultEvaluation = when (isMet) {
        true -> UnitResultEvaluation.PASSED
        false -> UnitResultEvaluation.NOT_PASSED
        null -> UnitResultEvaluation.NOT_EVALUATED
    }
}
