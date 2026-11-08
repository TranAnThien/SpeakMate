package com.project.speakmate.ui.practice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.practice.components.PracticeAppBar
import com.project.speakmate.ui.practice.components.PracticeTextCard
import com.project.speakmate.ui.practice.state.ScenarioPreparationUiState
import com.project.speakmate.ui.practice.state.ScenarioPreparationPresentation
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ScenarioPreparationScreen(
    state: ScenarioPreparationUiState,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.presentation == ScenarioPreparationPresentation.LOADING) {
        Column(modifier.fillMaxSize().background(DesignColor.Background)) {
            PracticeAppBar(stringResource(R.string.practice_preparation_header), onBack)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                SpeakMateLoadingIndicator(size = 40.dp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.practice_loading), style = DesignTypography.Body, color = DesignColor.Primary)
            }
        }
        return
    }
    if (state.presentation == ScenarioPreparationPresentation.ERROR) {
        Column(modifier.fillMaxSize().background(DesignColor.Background)) {
            PracticeAppBar(stringResource(R.string.practice_preparation_header), onBack)
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.practice_error_title), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Text(state.errorMessage, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                PrimaryButton(stringResource(R.string.content_reload), onRetry)
            }
        }
        return
    }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PracticeAppBar(stringResource(R.string.practice_preparation_header), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(state.title, style = DesignTypography.Heading, color = DesignColor.Primary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            val coverRes = state.coverRes
            if (coverRes != null) {
                Image(painterResource(coverRes), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop)
            }
            PracticeTextCard(stringResource(R.string.practice_context_heading), state.context, false)
            PracticeTextCard(stringResource(R.string.practice_ai_role_heading), state.aiRole, false)
            PracticeTextCard(stringResource(R.string.practice_hint_heading), state.hint, true)
        }
        Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (state.isInitializing) {
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp))
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    SpeakMateLoadingIndicator(size = 24.dp, strokeWidth = 2.dp)
                }
            } else {
                PrimaryButton(stringResource(R.string.practice_start_call), onStartCall)
            }
        }
    }
}
