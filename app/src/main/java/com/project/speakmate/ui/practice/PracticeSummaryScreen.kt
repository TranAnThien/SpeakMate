package com.project.speakmate.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.practice.components.PracticeAppBar
import com.project.speakmate.ui.practice.components.PracticeTextCard
import com.project.speakmate.ui.practice.state.PracticeSummaryUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PracticeSummaryScreen(
    state: PracticeSummaryUiState,
    onBack: () -> Unit,
    onViewTranscript: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PracticeAppBar(stringResource(R.string.practice_summary_header), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).background(DesignColor.ResultSuccessSurface, RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_placement_passed), null, Modifier.size(32.dp), tint = DesignColor.Success)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.practice_summary_title), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.practice_summary_description), style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PracticeTextCard(state.scenarioTitle, state.sessionDescription, true)
            }
        }
        Column(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.practice_view_transcript), onViewTranscript)
            SecondaryButton(stringResource(R.string.practice_go_home), onGoHome)
        }
    }
}
