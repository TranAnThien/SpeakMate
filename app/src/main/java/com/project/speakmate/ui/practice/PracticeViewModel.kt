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
import com.project.speakmate.utils.extension.suspendRunCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private var allPractices = emptyList<FreePractice>()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(presentation = ScenarioCatalogPresentation.LOADING) }

            allPractices = practiceRepository.getScenarioCatalog().getOrElse {
                _uiState.update { state -> state.copy(presentation = ScenarioCatalogPresentation.LOAD_ERROR) }
                return@launch
            }

            val distinctCategories = allPractices.map { it.category }.distinct()
            val filters = mutableListOf(ContentFilterUiState(id = AppConstant.PRACTICE_CATEGORY_ALL, label = ""))
            filters.addAll(distinctCategories.map { category ->
                ContentFilterUiState(id = category, label = category)
            })

            val scenarioItems = mapToUiState(allPractices).getOrElse {
                _uiState.update { state -> state.copy(presentation = ScenarioCatalogPresentation.LOAD_ERROR) }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    presentation = if (scenarioItems.isEmpty()) ScenarioCatalogPresentation.UNCONFIGURED else ScenarioCatalogPresentation.POPULATED,
                    categories = filters,
                    selectedCategoryId = AppConstant.PRACTICE_CATEGORY_ALL,
                    scenarios = scenarioItems
                )
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            val filteredPractices = if (categoryId == AppConstant.PRACTICE_CATEGORY_ALL) allPractices else allPractices.filter { it.category == categoryId }
            val filteredItems = mapToUiState(filteredPractices).getOrElse {
                _uiState.update { state -> state.copy(presentation = ScenarioCatalogPresentation.LOAD_ERROR) }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    scenarios = filteredItems,
                    presentation = if (filteredItems.isEmpty()) ScenarioCatalogPresentation.EMPTY else ScenarioCatalogPresentation.POPULATED
                )
            }
        }
    }

    private suspend fun mapToUiState(practices: List<FreePractice>): Result<List<ScenarioItemUiState>> = suspendRunCatching {
        coroutineScope { practices.map { fp -> async {
            val scenario = practiceRepository.getScenario(fp.scenarioId).getOrThrow()
            val metadata = practiceRepository.getScenarioPreparationMetadata(scenario.scenarioId).getOrNull()
            ScenarioItemUiState(
                id = fp.practiceId,
                title = scenario.name,
                categoryLabel = fp.category,
                roleLabel = metadata?.aiRole,
                description = scenario.description,
                imageUrl = metadata?.imageUrl,
                imageRes = null
            )
        } }.awaitAll() }
    }
}
