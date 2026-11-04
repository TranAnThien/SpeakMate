package com.project.speakmate.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.R
import com.project.speakmate.ui.components.ContentFilterUiState
import com.project.speakmate.ui.practice.state.PracticeSummaryUiState
import com.project.speakmate.ui.practice.state.ScenarioCatalogPresentation
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.practice.state.ScenarioItemUiState
import com.project.speakmate.ui.practice.state.ScenarioPreparationUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Free Practice / Scenario Selection / Populated / Source / 17:56", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogPopulatedScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.POPULATED),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "UX State Additions • Scenario Selection • Loading • Proposal / 33:2614", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogLoadingScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.LOADING),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "UX State Additions • Scenario Selection • Empty • Proposal / 33:2629", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogEmptyScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.EMPTY),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Free Practice / Scenario Selection / Config empty state / Added / 36:3750", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogUnconfiguredScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.UNCONFIGURED),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Free Practice / Catalog / Load Error / Retry / Audited / 33:2469", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogLoadErrorScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.LOAD_ERROR),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Free Practice / Catalog / Load Error / Dialog / Audited / 59:1371", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioCatalogErrorDialogScreenPreview() {
    SpeakMateTheme {
        ScenarioCatalogScreen(
            state = rememberCatalogPreviewState(ScenarioCatalogPresentation.ERROR_DIALOG),
            onSelectCategory = {},
            onSelectScenario = {},
            onRetry = {},
            onDismissError = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Free Practice / Scenario Preparation / Source / 17:57", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioPreparationScreenPreview() {
    SpeakMateTheme {
        ScenarioPreparationScreen(
            state = ScenarioPreparationUiState(
                title = stringResource(R.string.practice_preview_cafe_title),
                coverRes = R.drawable.img_practice_cafe_cover,
                context = stringResource(R.string.practice_preview_cafe_context),
                aiRole = stringResource(R.string.practice_preview_cafe_ai_role),
                hint = stringResource(R.string.practice_preview_cafe_hint),
                isInitializing = false
            ),
            onBack = {},
            onStartCall = {}
        )
    }
}

@Preview(name = "Free Practice / Start Call / Initializing / Added • UX gap P1 / 35:3717", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ScenarioInitializingScreenPreview() {
    SpeakMateTheme {
        ScenarioPreparationScreen(
            state = ScenarioPreparationUiState(
                title = stringResource(R.string.practice_preview_cafe_title),
                coverRes = R.drawable.img_practice_cafe_cover,
                context = stringResource(R.string.practice_preview_cafe_context),
                aiRole = stringResource(R.string.practice_preview_cafe_ai_role),
                hint = stringResource(R.string.practice_preview_cafe_hint),
                isInitializing = true
            ),
            onBack = {},
            onStartCall = {}
        )
    }
}

@Preview(name = "Free Practice / Summary / 17:58", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PracticeSummaryScreenPreview() {
    SpeakMateTheme {
        PracticeSummaryScreen(
            state = PracticeSummaryUiState(
                scenarioTitle = stringResource(R.string.practice_preview_cafe_title),
                sessionDescription = stringResource(R.string.practice_preview_session_description)
            ),
            onBack = {},
            onViewTranscript = {},
            onGoHome = {}
        )
    }
}

@Composable
private fun rememberCatalogPreviewState(presentation: ScenarioCatalogPresentation): ScenarioCatalogUiState = ScenarioCatalogUiState(
    presentation = presentation,
    categories = listOf(
        ContentFilterUiState("all", stringResource(R.string.content_filter_all)),
        ContentFilterUiState("work", stringResource(R.string.practice_category_work)),
        ContentFilterUiState("travel", stringResource(R.string.practice_category_travel)),
        ContentFilterUiState("lifestyle", stringResource(R.string.practice_category_lifestyle))
    ),
    selectedCategoryId = "all",
    scenarios = if (presentation == ScenarioCatalogPresentation.POPULATED) listOf(
        ScenarioItemUiState(
            id = "cafe",
            title = stringResource(R.string.practice_preview_cafe_title),
            categoryLabel = stringResource(R.string.practice_category_lifestyle),
            roleLabel = stringResource(R.string.practice_preview_cafe_role),
            description = stringResource(R.string.practice_preview_cafe_description),
            imageRes = R.drawable.img_practice_cafe_thumbnail
        ),
        ScenarioItemUiState(
            id = "hotel",
            title = stringResource(R.string.practice_preview_hotel_title),
            categoryLabel = stringResource(R.string.practice_category_travel),
            roleLabel = stringResource(R.string.practice_preview_hotel_role),
            description = stringResource(R.string.practice_preview_hotel_description),
            imageRes = R.drawable.img_practice_hotel_thumbnail
        ),
        ScenarioItemUiState(
            id = "office",
            title = stringResource(R.string.practice_preview_office_title),
            categoryLabel = stringResource(R.string.practice_category_work),
            roleLabel = stringResource(R.string.practice_preview_office_role),
            description = stringResource(R.string.practice_preview_office_description),
            imageRes = R.drawable.img_practice_office_thumbnail
        )
    ) else emptyList()
)
