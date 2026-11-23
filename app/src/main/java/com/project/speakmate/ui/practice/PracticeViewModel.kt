package com.project.speakmate.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.FreePractice
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.suspendRunCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScenarioCatalogUiState())
    val uiState: StateFlow<ScenarioCatalogUiState> = _uiState.asStateFlow()

    private var catalogItems = emptyList<ScenarioItemUiState>()
    private var catalogJob: Job? = null
    private var hasLoadedCatalog = false

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            _uiState.update { it.copy(presentation = if (hasLoadedCatalog) it.presentation else ScenarioCatalogPresentation.LOADING,
                isRefreshing = hasLoadedCatalog, hasRefreshError = false) }
            val result = suspendRunCatching { practiceRepository.getScenarioCatalog().getOrThrow() }
            ensureActive()
            val practices = result.getOrElse {
                _uiState.update { state -> state.copy(presentation = if (hasLoadedCatalog) state.presentation else ScenarioCatalogPresentation.LOAD_ERROR,
                    isRefreshing = false, hasRefreshError = hasLoadedCatalog) }
                return@launch
            }

            val distinctCategories = practices.map { it.category }.distinct()
            val filters = mutableListOf(ContentFilterUiState(id = AppConstant.PRACTICE_CATEGORY_ALL, label = ""))
            filters.addAll(distinctCategories.map { category ->
                ContentFilterUiState(id = category, label = category)
            })

            val mapped = mapToUiState(practices)
            ensureActive()
            val scenarioItems = mapped.getOrElse {
                _uiState.update { state -> state.copy(presentation = if (hasLoadedCatalog) state.presentation else ScenarioCatalogPresentation.LOAD_ERROR,
                    isRefreshing = false, hasRefreshError = hasLoadedCatalog) }
                return@launch
            }

            catalogItems = scenarioItems.distinctBy { it.id }
            hasLoadedCatalog = true
            _uiState.update { state ->
                val selectedId = state.selectedCategoryId.takeIf { id -> filters.any { it.id == id } }
                    .default(AppConstant.PRACTICE_CATEGORY_ALL)
                val filtered = filterItems(selectedId)
                state.copy(
                    presentation = getPresentation(filtered),
                    categories = filters,
                    selectedCategoryId = selectedId,
                    scenarios = filtered,
                    isRefreshing = false,
                    hasRefreshError = false
                )
            }
        }
    }

    fun selectCategory(categoryId: String) {
        if (_uiState.value.categories.none { it.id == categoryId }) return
        val filtered = filterItems(categoryId)
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = categoryId,
                scenarios = filtered,
                presentation = if (state.presentation == ScenarioCatalogPresentation.LOADING) state.presentation else getPresentation(filtered)
            )
        }
    }

    fun dismissError() {
        _uiState.update { state ->
            if (state.presentation == ScenarioCatalogPresentation.ERROR_DIALOG) state.copy(presentation = if (hasLoadedCatalog) getPresentation(state
                .scenarios) else ScenarioCatalogPresentation.LOAD_ERROR) else state
        }
    }

    private fun filterItems(categoryId: String): List<ScenarioItemUiState> =
        if (categoryId == AppConstant.PRACTICE_CATEGORY_ALL) catalogItems else catalogItems.filter { it.categoryLabel == categoryId }

    private fun getPresentation(items: List<ScenarioItemUiState>): ScenarioCatalogPresentation = when {
        catalogItems.isEmpty() -> ScenarioCatalogPresentation.UNCONFIGURED
        items.isEmpty() -> ScenarioCatalogPresentation.EMPTY
        else -> ScenarioCatalogPresentation.POPULATED
    }

    private suspend fun mapToUiState(practices: List<FreePractice>): Result<List<ScenarioItemUiState>> = suspendRunCatching {
        coroutineScope { practices.map { fp -> async {
            val scenario = practiceRepository.getScenario(fp.scenarioId).getOrThrow()
            val metadataResult = practiceRepository.getScenarioPreparationMetadata(scenario.scenarioId)
            metadataResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val metadata = metadataResult.getOrNull()
            ScenarioItemUiState(
                id = fp.practiceId,
                title = scenario.name,
                categoryLabel = fp.category,
                description = scenario.description,
                imageUrl = metadata?.imageUrl,
                imageRes = null,
                suggestedLevel = metadata?.suggestedLevel,
                estimatedDurationMinutes = metadata?.estimatedDurationMinutes
            )
        } }.awaitAll() }
    }
}
