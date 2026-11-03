package com.project.speakmate.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.LoadErrorDialog
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.StateIllustration
import com.project.speakmate.ui.home.components.HomeLoadingContent
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
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DesignColor.Background)) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(state.presentation == HomePresentation.LOADING, onOpenProfile)
            Box(Modifier.weight(1f)) {
                when (state.presentation) {
                    HomePresentation.LOADING -> HomeLoadingContent()
                    HomePresentation.ERROR, HomePresentation.ERROR_DIALOG -> HomeUnavailableContent(
                        state.presentation == HomePresentation.ERROR_DIALOG, onRetry
                    )
                    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (state.hasProfileSummary) HomeProfileCard(state.displayName, state.email, onOpenProfile)
                        HomeLevelCard(state.level, state.presentation == HomePresentation.UNASSESSED)
                        when (state.presentation) {
                            HomePresentation.UNASSESSED -> HomeAssessmentCard(onStartAssessment)
                            HomePresentation.ACTIVE -> HomeCurrentLearningCard(state.goalTitle, state.unitTitle, state.unitDescription)
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
            AppBottomBar(onOpenTrainingPath, onOpenPractice, onOpenHistory, onOpenProfile)
        }
        if (state.presentation == HomePresentation.ERROR_DIALOG) {
            LoadErrorDialog(stringResource(R.string.home_error_dialog_title), stringResource(R.string.home_error_dialog_description),
                onRetry, titleStyle = DesignTypography.SectionTitle, descriptionStyle = DesignTypography.EmptyBody,
                actionMinHeight = 52.dp, actionSpacing = 23.dp)
        }
    }
}

@Composable
private fun HomeHeader(isLoading: Boolean, onOpenProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.app_name), style = DesignTypography.Brand, color = DesignColor.Primary)
        Box(Modifier.size(40.dp).background(if (isLoading) DesignColor.Skeleton else DesignColor.IconBlue, CircleShape)
            .clickable(enabled = !isLoading, onClick = onOpenProfile), contentAlignment = Alignment.Center) {
            if (!isLoading) Image(painterResource(R.drawable.ic_navigation_profile), null, Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(DesignColor.Primary))
        }
    }
}

@Composable
private fun HomeProfileCard(displayName: String, email: String, onOpenProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp))
        .clickable(onClick = onOpenProfile).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.size(48.dp).background(DesignColor.IconBlue, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_navigation_profile), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Primary))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(displayName, style = DesignTypography.Title, color = DesignColor.Text)
            Text(email, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
    }
}

@Composable
private fun HomeLevelCard(level: String, isUnassessed: Boolean) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.placement_estimated_level), style = DesignTypography.Label, color = DesignColor.Muted)
                Text(if (isUnassessed) stringResource(R.string.home_unassessed) else level,
                    style = DesignTypography.Brand, color = DesignColor.Primary)
            }
            Box(Modifier.size(40.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(if (isUnassessed) R.drawable.ic_placement_microphone else R.drawable.ic_home_speaking),
                    null, Modifier.size(24.dp))
            }
        }
        if (isUnassessed) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_assessment_summary), style = DesignTypography.Detail, color = DesignColor.Muted)
        }
    }
}

@Composable
private fun HomeAssessmentCard(onStartAssessment: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.home_assessment_title), style = DesignTypography.Title, color = DesignColor.Text)
        Text(stringResource(R.string.home_assessment_description), style = DesignTypography.Detail, color = DesignColor.Muted)
        PrimaryButton(stringResource(R.string.placement_start), onStartAssessment)
    }
}

@Composable
private fun HomeCurrentLearningCard(goalTitle: String, unitTitle: String, unitDescription: String) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_current_learning), style = DesignTypography.Label, color = DesignColor.Muted)
        Text(goalTitle, style = DesignTypography.Title, color = DesignColor.Text)
        Text(stringResource(R.string.home_current_unit), style = DesignTypography.Label, color = DesignColor.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(unitTitle, style = DesignTypography.Title, color = DesignColor.Text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.training_current), style = DesignTypography.Badge, color = DesignColor.Primary,
                modifier = Modifier.background(DesignColor.IconBlue, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 7.dp))
        }
        Text(unitDescription, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}

@Composable
private fun HomeCompletionCard(
    level: String,
    nextLevel: String,
    isHighestLevel: Boolean,
    onStartReassessment: () -> Unit,
    onReviewPath: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_path_completed, level), style = DesignTypography.Title, color = DesignColor.Text)
        Text(if (isHighestLevel) stringResource(R.string.home_highest_level_description)
            else stringResource(R.string.home_path_completed_description, nextLevel),
            style = DesignTypography.Detail, color = DesignColor.Muted)
        PrimaryButton(if (isHighestLevel) stringResource(R.string.home_review_content)
            else stringResource(R.string.home_reassess, nextLevel), if (isHighestLevel) onReviewPath else onStartReassessment)
    }
}

@Composable
private fun HomeFreePracticeCard(onSelectScenario: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_free_practice), style = DesignTypography.Title, color = DesignColor.Text)
                Text(stringResource(R.string.home_free_practice_description), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            Box(Modifier.size(40.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_navigation_practice), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Primary))
            }
        }
        Text(stringResource(R.string.home_free_practice_notice), style = DesignTypography.Detail, color = DesignColor.Primary,
            modifier = Modifier.fillMaxWidth().background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
        HomeTextAction(stringResource(R.string.home_select_scenario), onSelectScenario)
    }
}

@Composable
private fun HomeReviewCard(onReviewPath: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_review_content), style = DesignTypography.Title, color = DesignColor.Text)
        Text(stringResource(R.string.home_review_description), style = DesignTypography.Detail, color = DesignColor.Muted)
        HomeTextAction(stringResource(R.string.home_view_path), onReviewPath)
    }
}

@Composable
private fun HomeTextAction(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.CenterStart) {
        Text(text, style = DesignTypography.Link, color = DesignColor.Primary)
    }
}

@Composable
private fun HomeUnavailableContent(hasDialog: Boolean, onRetry: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val contentHeight = maxHeight
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
            .padding(horizontal = 40.dp).padding(top = 24.dp, bottom = 48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StateIllustration(R.drawable.ic_home_dashboard)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(if (hasDialog) R.string.home_empty_title else R.string.home_error_title),
                    style = DesignTypography.SectionTitle, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(if (hasDialog) R.string.home_empty_description else R.string.home_error_description),
                    style = DesignTypography.EmptyBody, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(52.dp))
                if (!hasDialog) PrimaryButton(stringResource(R.string.home_retry), onRetry, minHeight = 52.dp)
            }
        }
    }
}
