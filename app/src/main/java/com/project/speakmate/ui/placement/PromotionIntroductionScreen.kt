package com.project.speakmate.ui.placement

import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.placement.components.PromotionStepsCard
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PromotionIntroductionScreen(
    state: PromotionIntroductionUiState,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.promotion_header), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.currentLevelLabel.isNotBlank()) Text(stringResource(R.string.assessment_completed_path, state.currentLevelLabel),
                style = DesignTypography.Label, color = DesignColor.Muted)
            Text(state.title, style = DesignTypography.Heading, color = DesignColor.Text)
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp))
                .border(1.dp, DesignColor.Border, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.assessment_each_call_heading), style = DesignTypography.Title, color = DesignColor.Text)
                Text(state.description, style = DesignTypography.Body, color = DesignColor.Muted)
            }
            Column(Modifier.fillMaxWidth().background(DesignColor.SurfaceContainer, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.conditionTitle, style = DesignTypography.Title, color = DesignColor.Text)
                Text(state.conditionDescription, style = DesignTypography.Body, color = DesignColor.Muted)
                Text(stringResource(R.string.placement_result_disclaimer), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            PromotionStepsCard(state.steps)
        }
        Column(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.placement_start), onStartAssessment)
            TextButton(onClick = onReturnToPath, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.goal_assessment_return_to_path), style = DesignTypography.Button)
            }
        }
    }
}
