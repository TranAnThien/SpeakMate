package com.project.speakmate.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
import com.project.speakmate.ui.components.ContentErrorDialog
import com.project.speakmate.ui.components.ContentFilterRow
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.components.ContentPageHeader
import com.project.speakmate.ui.components.ContentSkeletonCards
import com.project.speakmate.ui.components.ContentStatusPanel
import com.project.speakmate.ui.practice.components.ScenarioCard
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ScenarioCatalogScreen(
    state: ScenarioCatalogUiState,
    onSelectCategory: (String) -> Unit,
    onSelectScenario: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            ContentPageHeader(stringResource(R.string.navigation_practice))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "catalog-header", contentType = ContentFilterUiState::class) {
                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(stringResource(R.string.practice_learning_notice), style = DesignTypography.Detail, color = DesignColor.Primary,
                            modifier = Modifier.fillMaxWidth().background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp)).padding(16.dp))
                        if (state.presentation != ScenarioCatalogPresentation.UNCONFIGURED) {
                            ContentFilterRow(state.categories, state.selectedCategoryId, onSelectCategory)
                        }
                    }
                }
                when (state.presentation) {
                    ScenarioCatalogPresentation.POPULATED -> itemsIndexed(state.scenarios,
                        key = { index, scenario -> "scenario:$index:${scenario.id}" },
                        contentType = { _, _ -> ScenarioItemUiState::class }) { _, scenario ->
                        val onSelect = remember(scenario.id, onSelectScenario) { { onSelectScenario(scenario.id) } }
                        ScenarioCard(scenario, onSelect)
                    }
                    ScenarioCatalogPresentation.LOADING -> item(key = "catalog-loading", contentType = ScenarioCatalogPresentation.LOADING) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(stringResource(R.string.practice_loading), style = DesignTypography.Detail, color = DesignColor.Primary)
                            ContentSkeletonCards(3)
                        }
                    }
                    ScenarioCatalogPresentation.EMPTY, ScenarioCatalogPresentation.UNCONFIGURED,
                    ScenarioCatalogPresentation.LOAD_ERROR -> item(key = "catalog-status", contentType = state.presentation) {
                        ContentStatusPanel(
                            iconRes = if (state.presentation == ScenarioCatalogPresentation.LOAD_ERROR) R.drawable.ic_load_error else R.drawable.ic_practice_empty_catalog,
                            title = stringResource(when (state.presentation) {
                                ScenarioCatalogPresentation.EMPTY -> R.string.practice_empty_title
                                ScenarioCatalogPresentation.UNCONFIGURED -> R.string.practice_unconfigured_title
                                else -> R.string.practice_error_title
                            }),
                            description = stringResource(when (state.presentation) {
                                ScenarioCatalogPresentation.EMPTY -> R.string.practice_empty_description
                                ScenarioCatalogPresentation.UNCONFIGURED -> R.string.practice_unconfigured_description
                                else -> R.string.practice_error_description
                            }),
                            primaryText = stringResource(if (state.presentation == ScenarioCatalogPresentation.UNCONFIGURED)
                                R.string.goal_assessment_return_to_path else R.string.content_reload),
                            secondaryText = stringResource(if (state.presentation == ScenarioCatalogPresentation.UNCONFIGURED)
                                R.string.content_reload else R.string.goal_assessment_return_to_path),
                            onPrimaryAction = if (state.presentation == ScenarioCatalogPresentation.UNCONFIGURED) onOpenTrainingPath else onRetry,
                            onSecondaryAction = if (state.presentation == ScenarioCatalogPresentation.UNCONFIGURED) onRetry else onOpenTrainingPath
                        )
                    }
                    ScenarioCatalogPresentation.ERROR_DIALOG -> Unit
                }
            }
            AppBottomBar(onOpenTrainingPath, onOpenPractice, onOpenHistory, onOpenProfile,
                selectedDestination = AppDestination.PRACTICE, bottomPadding = 4.dp)
        }
        if (state.presentation == ScenarioCatalogPresentation.ERROR_DIALOG) {
            ContentErrorDialog(stringResource(R.string.practice_error_title), stringResource(R.string.practice_error_dialog_description),
                onRetry, onDismissError)
        }
    }
}
