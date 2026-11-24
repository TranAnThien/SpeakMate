package com.project.speakmate.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.assessment.rememberSpeakingLevelLabel
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.image.ScenarioImage
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.practice.components.PracticeTextCard
import com.project.speakmate.ui.practice.state.ScenarioPreparationLoadError
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import com.project.speakmate.ui.practice.state.ScenarioPreparationUiState
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun ScenarioPreparationScreen(
    state: ScenarioPreparationUiState,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
    noticeContent: @Composable () -> Unit = {}
) {
    if (state.presentation == ScenarioPreparationPresentation.LOADING) {
        Column(modifier.fillMaxSize().background(DesignColor.Background)) {
            BackAppBar(stringResource(R.string.practice_preparation_header), onBack)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpeakMateLoadingIndicator(size = 40.dp)
                Spacer(Modifier.height(16.dp))
                BodyText(text = stringResource(R.string.practice_loading), color = DesignColor.Primary)
            }
            noticeContent()
        }
        return
    }
    if (state.presentation == ScenarioPreparationPresentation.ERROR) {
        Column(modifier.fillMaxSize().background(DesignColor.Background)) {
            BackAppBar(stringResource(R.string.practice_preparation_header), onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeadingText(text = stringResource(R.string.practice_error_title), textAlign = TextAlign.Center)
                BodyText(
                    text =
                        stringResource(
                            if (state.loadError == ScenarioPreparationLoadError.REMOVED)
                                R.string.preparation_activity_unavailable
                            else R.string.practice_error_description
                        ),
                    color = DesignColor.Muted,
                    textAlign = TextAlign.Center
                )
            }
            noticeContent()
            Box(Modifier
                .fillMaxWidth()
                .background(DesignColor.Surface)
                .padding(horizontal = 24.dp, vertical = 16.dp)) {
                PrimaryButton(stringResource(R.string.content_reload), onRetry)
            }
        }
        return
    }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.practice_preparation_header), onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeadingText(
                text = state.title,
                color = DesignColor.Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            ScenarioImage(state.imageUrl, Modifier.fillMaxWidth().height(180.dp))
            state.suggestedLevel?.let {
                DetailText(text = stringResource(R.string.practice_suggested_level, rememberSpeakingLevelLabel(it)))
            }
            state.estimatedDurationMinutes?.let {
                DetailText(text = pluralStringResource(R.plurals.practice_duration_minutes, it, it))
            }
            if (state.hasRefreshError) {
                DetailText(
                    text =
                        stringResource(
                            if (state.loadError == ScenarioPreparationLoadError.REMOVED)
                                R.string.preparation_activity_unavailable
                            else R.string.preparation_refresh_error
                        ),
                    color = DesignColor.Error
                )
                SecondaryButton(stringResource(R.string.content_reload), onRetry)
            }
            PracticeTextCard(
                title = stringResource(R.string.practice_context_heading),
                text = state.context,
                isHighlighted = false
            )
            if (state.aiRole.isNotBlank())
                PracticeTextCard(
                    title = stringResource(R.string.practice_ai_role_heading),
                    text = state.aiRole,
                    isHighlighted = false
                )
            if (state.preCallHint.isNotBlank())
                PracticeTextCard(
                    title = stringResource(R.string.practice_hint_heading),
                    text = state.preCallHint,
                    isHighlighted = true
                )
        }
        noticeContent()
        Box(Modifier
            .fillMaxWidth()
            .background(DesignColor.Surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (state.isStarting) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpeakMateLoadingIndicator(size = 24.dp, strokeWidth = 2.dp)
                }
            } else {
                PrimaryButton(
                    text = stringResource(R.string.practice_start_call),
                    onClick = onStartCall,
                    enabled = state.scenario != null && state.loadError != ScenarioPreparationLoadError.REMOVED
                )
            }
        }
    }
}
