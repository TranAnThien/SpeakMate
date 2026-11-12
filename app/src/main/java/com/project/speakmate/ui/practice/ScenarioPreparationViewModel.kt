package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.practice.state.ScenarioPreparationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScenarioPreparationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val practiceId: String = savedStateHandle.get<String>("practiceId") ?: ""

    private val _uiState = MutableStateFlow(ScenarioPreparationUiState())
    val uiState: StateFlow<ScenarioPreparationUiState> = _uiState.asStateFlow()

    init {
        loadScenarioDetails()
    }

    fun loadScenarioDetails() {
        viewModelScope.launch {
            _uiState.update { ScenarioPreparationUiState(presentation = com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation.LOADING, practiceId = practiceId) }
            val practice = practiceRepository.getScenarioCatalog().getOrElse { error ->
                showError(error)
                return@launch
            }.firstOrNull { it.practiceId == practiceId } ?: run {
                showError(IllegalStateException("Free Practice '$practiceId' does not exist."))
                return@launch
            }
            val scenario = practiceRepository.getScenario(practice.scenarioId).getOrElse { error ->
                showError(error)
                return@launch
            }
            val metadata = practiceRepository.getScenarioPreparationMetadata(scenario.scenarioId).getOrElse { error ->
                showError(error)
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    presentation = com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation.SUCCESS,
                    practiceId = practiceId,
                    scenarioId = scenario.scenarioId,
                    title = scenario.name,
                    imageUrl = metadata.getValue("imageUrl"),
                    context = metadata.getValue("uiContext"),
                    aiRole = metadata.getValue("aiRole"),
                    preCallHint = metadata.getValue("preCallHint"),
                    scenario = scenario,
                    isInitializing = false
                )
            }
        }
    }

    fun onStartCallPressed() {
        _uiState.update { state ->
            if (state.presentation == com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation.SUCCESS) {
                state.copy(isInitializing = true)
            } else state
        }
    }

    /** Restores the preparation CTA after its call destination has closed. */
    fun onCallExited() {
        _uiState.update { it.copy(isInitializing = false) }
    }

    private fun showError(error: Throwable) {
        _uiState.update {
            it.copy(
                presentation = com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation.ERROR,
                errorMessage = error.message ?: "Không thể nạp Scenario này.",
                isInitializing = false
            )
        }
    }
}
