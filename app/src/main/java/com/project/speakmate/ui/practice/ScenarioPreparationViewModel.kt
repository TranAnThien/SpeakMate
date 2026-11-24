package com.project.speakmate.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.practice.state.ScenarioPreparationLoadError
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import com.project.speakmate.ui.practice.state.ScenarioPreparationUiState
import com.project.speakmate.utils.extension.defaultEmpty
import com.project.speakmate.utils.extension.defaultFalse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScenarioPreparationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val route = try { savedStateHandle.toRoute<AppRoute.ScenarioPreparation>() } catch (_: IllegalArgumentException) { null }
    private val practiceId = route?.practiceId.defaultEmpty()
    private val shouldRequireServer = route?.isCalling.defaultFalse()
    private val _uiState = MutableStateFlow(ScenarioPreparationUiState())
    val uiState: StateFlow<ScenarioPreparationUiState> = _uiState.asStateFlow()
    private var preparationJob: Job? = null
    private var preparedUserId: String? = null

    init { loadScenarioDetails() }

    fun loadScenarioDetails() {
        preparedUserId = null
        cancelStart()
        preparationJob?.cancel()
        preparationJob = viewModelScope.launch {
            _uiState.update { if (it.scenario != null) it.copy(hasRefreshError = false) else ScenarioPreparationUiState(practiceId = practiceId) }
            try {
                val uid = authRepository.requireCurrentUserId()
                val practice = practiceRepository.getScenarioCatalog(requireServer = shouldRequireServer).getOrThrow().firstOrNull { it.practiceId == practiceId }
                if (practice == null) throw NoSuchElementException()
                val scenario = practiceRepository.getScenario(practice.scenarioId, requireServer = shouldRequireServer).getOrThrow()
                check(scenario.scenarioId == practice.scenarioId && scenario.name.isNotBlank() && scenario.promptTemplate.isNotBlank())
                val metadata = practiceRepository.getScenarioPreparationMetadata(scenario.scenarioId).getOrThrow()
                check(metadata.aiRole.isNotBlank())
                ensureActive()
                check(authRepository.requireCurrentUserId() == uid)
                preparedUserId = uid
                _uiState.update { it.copy(presentation = ScenarioPreparationPresentation.SUCCESS, loadError = null, practiceId = practiceId,
                    scenarioId = scenario.scenarioId, title = scenario.name, imageUrl = metadata.imageUrl,
                    context = metadata.uiContext.ifBlank { scenario.description }, aiRole = metadata.aiRole, preCallHint = metadata.preCallHint,
                    scenario = scenario, isStarting = false, hasRefreshError = false,
                    suggestedLevel = metadata.suggestedLevel, estimatedDurationMinutes = metadata.estimatedDurationMinutes) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { showError(failure is NoSuchElementException) }
        }
    }

    fun prepareCall() {
        val state = _uiState.value
        val scenario = state.scenario ?: return
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { return }
        if (uid != preparedUserId || state.isStarting || state.isStartReady || state.hasRefreshError ||
            state.presentation != ScenarioPreparationPresentation.SUCCESS || scenario.scenarioId != state.scenarioId ||
            scenario.scenarioId.isBlank() || scenario.name.isBlank() || scenario.promptTemplate.isBlank()) return
        _uiState.update { it.copy(isStarting = true, isStartReady = true) }
    }

    fun consumeStartReady(): Boolean {
        val state = _uiState.value
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { return false }
        return uid == preparedUserId && state.isStartReady && _uiState.compareAndSet(state, state.copy(isStartReady = false))
    }

    fun cancelStart() { _uiState.update { it.copy(isStarting = false, isStartReady = false) } }

    private fun showError(isRemoved: Boolean = false) {
        _uiState.update { it.copy(presentation = if (it.scenario == null) ScenarioPreparationPresentation.ERROR else ScenarioPreparationPresentation.SUCCESS,
            loadError = if (isRemoved) ScenarioPreparationLoadError.REMOVED else ScenarioPreparationLoadError.UNAVAILABLE,
                hasRefreshError = it.scenario != null, isStarting = false) }
    }
}
