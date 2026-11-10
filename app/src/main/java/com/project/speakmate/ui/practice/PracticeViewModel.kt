package com.project.speakmate.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
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

    private var allPractices = emptyList<com.project.speakmate.domain.model.FreePractice>()

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
            val filters = mutableListOf(ContentFilterUiState(id = "ALL", label = "Tất cả"))
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
                    selectedCategoryId = "ALL",
                    scenarios = scenarioItems
                )
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            val filteredPractices = if (categoryId == "ALL") allPractices else allPractices.filter { it.category == categoryId }
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

    private suspend fun mapToUiState(practices: List<com.project.speakmate.domain.model.FreePractice>): Result<List<ScenarioItemUiState>> = runCatching {
        coroutineScope { practices.map { fp -> async {
            val scenario = practiceRepository.getScenario(fp.scenarioId).getOrThrow()
            val metadata = practiceRepository.getScenarioPreparationMetadata(scenario.scenarioId).getOrNull()
            ScenarioItemUiState(
                id = fp.practiceId,
                title = scenario.name,
                categoryLabel = fp.category,
                roleLabel = metadata?.get("aiRole") ?: "Luyện hội thoại tự do",
                description = scenario.description,
                imageUrl = metadata?.get("imageUrl"),
                imageRes = null
            )
        } }.awaitAll() }
    }
}
