package com.project.speakmate.ui.placement

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PlacementIntroductionScreen(
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onSkipAssessment: () -> Unit,
    modifier: Modifier = Modifier,
    hasSaveFailed: Boolean = false,
    isSaving: Boolean = false
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.placement_title), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CaptionText(
                text = stringResource(R.string.assessment_introduction_eyebrow),
                style = DesignTypography.Label
            )
            HeadingText(text = stringResource(R.string.placement_introduction_title))
            SpeakMateCard(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(width = 1.dp, color = DesignColor.Border, shape = RoundedCornerShape(16.dp)),
                contentPadding = 16.dp,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TitleText(text = stringResource(R.string.assessment_conversation_heading))
                BodyText(
                    text = stringResource(R.string.placement_introduction_description),
                    color = DesignColor.Muted
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(DesignColor.SurfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TitleText(text = stringResource(R.string.assessment_estimate_heading))
                DetailText(text = stringResource(R.string.placement_introduction_disclaimer))
                DetailText(text = stringResource(R.string.placement_no_history))
            }
            TitleText(text = stringResource(R.string.promotion_before_start))
            PlacementRequirementRow(
                iconRes = R.drawable.ic_placement_quiet,
                text = stringResource(R.string.placement_quiet_space),
                description = stringResource(R.string.assessment_quiet_detail)
            )
            PlacementRequirementRow(
                iconRes = R.drawable.ic_placement_microphone,
                text = stringResource(R.string.placement_microphone),
                description = stringResource(R.string.assessment_microphone_detail)
            )
            PlacementRequirementRow(
                iconRes = R.drawable.ic_navigation_practice,
                text = stringResource(R.string.assessment_natural_speech),
                description = stringResource(R.string.assessment_natural_speech_detail)
            )
            if (hasSaveFailed)
                DetailText(text = stringResource(R.string.placement_save_failed), color = DesignColor.Error)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = stringResource(R.string.placement_start),
                onClick = onStartAssessment,
                enabled = !isSaving
            )
            if (isSaving) SpeakMateLoadingIndicator()
            TextButton(onClick = onSkipAssessment, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                DetailText(
                    text = stringResource(if (isSaving) R.string.placement_saving else R.string.placement_later),
                    style = DesignTypography.Button
                )
            }
        }
    }
}

@Composable
private fun PlacementRequirementRow(@DrawableRes iconRes: Int, text: String, description: String) {
    SpeakMateCard(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BodyText(text = text)
                DetailText(text = description)
            }
        }
    }
}
