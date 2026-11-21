package com.project.speakmate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.LoadErrorDialog
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.UserProfileLoadError
import com.project.speakmate.ui.home.components.HomeAssessmentCard
import com.project.speakmate.ui.home.components.HomeCompletionCard
import com.project.speakmate.ui.home.components.HomeCurrentLearningCard
import com.project.speakmate.ui.home.components.HomeFreePracticeCard
import com.project.speakmate.ui.home.components.HomeHeader
import com.project.speakmate.ui.home.components.HomeLevelCard
import com.project.speakmate.ui.home.components.HomeLoadingContent
import com.project.speakmate.ui.home.components.HomeProfileCard
import com.project.speakmate.ui.home.components.HomeReviewCard
import com.project.speakmate.ui.home.components.HomeUnavailableContent
import com.project.speakmate.ui.home.state.HomePresentation
import com.project.speakmate.ui.home.state.HomeUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartAssessment: () -> Unit,
    onContinuePath: () -> Unit,
    onStartReassessment: () -> Unit,
    onReviewPath: () -> Unit,
    onSelectScenario: () -> Unit,
    onRetry: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            HomeHeader(state.presentation == HomePresentation.LOADING, state.avatarUrl, onViewProfile)
            Box(Modifier.weight(1f)) {
                when (state.presentation) {
                    HomePresentation.LOADING -> HomeLoadingContent()
                    HomePresentation.PROFILE_LOAD_ERROR -> UserProfileLoadError(onRetry, Modifier.padding(24.dp))
                    HomePresentation.ERROR, HomePresentation.ERROR_DIALOG -> HomeUnavailableContent(
                        state.presentation == HomePresentation.ERROR_DIALOG, onRetry
                    )
                    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (state.hasProfileSummary) HomeProfileCard(state.displayName, state.email, state.avatarUrl, onViewProfile)
                        HomeLevelCard(state.level, state.presentation == HomePresentation.UNASSESSED)
                        when (state.presentation) {
                            HomePresentation.UNASSESSED -> HomeAssessmentCard(onStartAssessment)
                            HomePresentation.ACTIVE -> HomeCurrentLearningCard(state.goalTitle, state.unitTitle, if (state.isGoalAssessment) stringResource(R.string.home_goal_assessment_description) else state.unitDescription)
                            HomePresentation.PATH_COMPLETE, HomePresentation.HIGHEST_LEVEL_COMPLETE -> HomeCompletionCard(
                                state.level, state.nextLevel, state.presentation == HomePresentation.HIGHEST_LEVEL_COMPLETE,
                                onStartReassessment, onReviewPath
                            )
                            else -> Unit
                        }
                        if (state.presentation == HomePresentation.PATH_COMPLETE) HomeReviewCard(onReviewPath)
                        else HomeFreePracticeCard(onSelectScenario)
                    }
                }
            }
            if (state.hasContinueAction || state.presentation == HomePresentation.LOADING) {
                Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 12.dp)) {
                    if (state.presentation == HomePresentation.LOADING) {
                        Box(Modifier.fillMaxWidth().height(56.dp).background(DesignColor.SkeletonButton, RoundedCornerShape(16.dp)))
                    } else PrimaryButton(stringResource(R.string.home_continue_path), onContinuePath)
                }
            }

        }
        if (state.presentation == HomePresentation.ERROR_DIALOG) {
            LoadErrorDialog(stringResource(R.string.home_error_dialog_title), stringResource(R.string.home_error_dialog_description),
                onRetry, titleStyle = DesignTypography.SectionTitle, descriptionStyle = DesignTypography.EmptyBody,
                actionMinHeight = 52.dp, actionSpacing = 23.dp)
        }
    }
}
