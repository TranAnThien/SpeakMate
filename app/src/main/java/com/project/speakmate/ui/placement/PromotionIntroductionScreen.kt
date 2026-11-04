package com.project.speakmate.ui.placement

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
import com.project.speakmate.ui.placement.components.PlacementAppBar
import com.project.speakmate.ui.placement.components.PlacementSecondaryButton
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
        PlacementAppBar(stringResource(R.string.promotion_header), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.img_placement_mascot), null, Modifier.size(80.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(state.description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
            }
            PromotionStepsCard(state.steps)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.promotion_condition_heading), style = DesignTypography.Title, color = DesignColor.Text)
                Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.conditionTitle, style = DesignTypography.Title, color = DesignColor.Text)
                    Text(state.conditionDescription, style = DesignTypography.Detail, color = DesignColor.Muted)
                }
            }
            Column(Modifier.fillMaxWidth().background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.promotion_before_start), style = DesignTypography.Title, color = DesignColor.Text)
                Text(state.note, style = DesignTypography.Detail, color = DesignColor.Primary)
            }
        }
        Column(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.placement_start), onStartAssessment)
            PlacementSecondaryButton(stringResource(R.string.goal_assessment_return_to_path), onReturnToPath)
        }
    }
}
