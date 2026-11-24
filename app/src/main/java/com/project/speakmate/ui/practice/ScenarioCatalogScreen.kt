package com.project.speakmate.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.project.speakmate.ui.components.ContentFilterRow
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.ContentPageHeader
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.practice.components.ScenarioCard
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun ScenarioCatalogScreen(
    state: ScenarioCatalogUiState,
    onSelectCategory: (String) -> Unit,
    onSelectScenario: (String) -> Unit,
    onRetry: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            ContentPageHeader(stringResource(R.string.navigation_practice))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "catalog-header", contentType = ContentFilterUiState::class) {
                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailText(
                            text = stringResource(R.string.practice_learning_notice),
                            color = DesignColor.Primary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                        )
                        if (state.presentation != ScenarioCatalogPresentation.UNCONFIGURED) {
                            ContentFilterRow(
                                filters =
                                    state.categories.map { category ->
                                        if (category.id == AppConstant.PRACTICE_CATEGORY_ALL)
                                            category.copy(label = stringResource(R.string.practice_category_all))
                                        else category
                                    },
                                selectedId = state.selectedCategoryId,
                                onSelectFilter = onSelectCategory
                            )
                        }
                    }
                }
                if (state.presentation == ScenarioCatalogPresentation.POPULATED)
                    item(key = "catalog-refresh", contentType = ScenarioCatalogUiState::class) {
                        if (state.isRefreshing) SpeakMateLoadingIndicator()
                        if (state.hasRefreshError)
                            DetailText(
                                text = stringResource(R.string.preparation_refresh_error),
                                color = DesignColor.Error
                            )
                        SecondaryButton(
                            text = stringResource(R.string.content_reload),
                            onClick = onRetry,
                            enabled = !state.isRefreshing
                        )
                    }
                when (state.presentation) {
                    ScenarioCatalogPresentation.POPULATED ->
                        itemsIndexed(
                            items = state.scenarios,
                            key = { _, scenario -> "scenario:${scenario.id}" },
                            contentType = { _, _ -> ScenarioItemUiState::class }
                        ) { _, scenario ->
                            val onSelect =
                                remember(scenario.id, onSelectScenario) { { onSelectScenario(scenario.id) } }
                            ScenarioCard(scenario, onSelect)
                        }
                    ScenarioCatalogPresentation.LOADING ->
                        item(key = "catalog-loading", contentType = ScenarioCatalogPresentation.LOADING) {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                DetailText(
                                    text = stringResource(R.string.practice_loading),
                                    color = DesignColor.Primary
                                )
                                SpeakMateLoadingIndicator()
                            }
                        }
                    ScenarioCatalogPresentation.EMPTY,
                    ScenarioCatalogPresentation.UNCONFIGURED,
                    ScenarioCatalogPresentation.LOAD_ERROR ->
                        item(key = "catalog-status", contentType = state.presentation) {
                            StatusContent(
                                title =
                                    stringResource(
                                        when (state.presentation) {
                                            ScenarioCatalogPresentation.EMPTY -> R.string.practice_empty_title
                                            ScenarioCatalogPresentation.UNCONFIGURED ->
                                                R.string.practice_unconfigured_title
                                            else -> R.string.practice_error_title
                                        }
                                    ),
                                titleStyle = DesignTypography.Heading,
                                titleColor = DesignColor.Text,
                                description =
                                    stringResource(
                                        when (state.presentation) {
                                            ScenarioCatalogPresentation.EMPTY -> R.string.practice_empty_description
                                            ScenarioCatalogPresentation.UNCONFIGURED ->
                                                R.string.practice_unconfigured_description
                                            else -> R.string.practice_error_description
                                        }
                                    ),
                                descriptionStyle = DesignTypography.Body,
                                descriptionColor = DesignColor.Muted,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                illustration = {
                                    AnimatedMascot(
                                        animation =
                                            if (state.presentation == ScenarioCatalogPresentation.LOAD_ERROR)
                                                MascotAnimation.NETWORK_ERROR
                                            else MascotAnimation.EMPTY_STATE,
                                        modifier = Modifier.fillMaxWidth().height(120.dp)
                                    )
                                },
                                actions = {
                                    PrimaryButton(stringResource(R.string.content_reload), onRetry)
                                    SecondaryButton(
                                        stringResource(R.string.goal_assessment_return_to_path),
                                        onReturnToPath
                                    )
                                }
                            )
                        }
                }
            }
        }
    }
}
