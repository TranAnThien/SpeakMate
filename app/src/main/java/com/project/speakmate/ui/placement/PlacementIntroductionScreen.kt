package com.project.speakmate.ui.placement

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.assessment_introduction_eyebrow), style = DesignTypography.Label, color = DesignColor.Muted)
            Text(stringResource(R.string.placement_introduction_title), style = DesignTypography.Heading, color = DesignColor.Text)
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp))
                .border(1.dp, DesignColor.Border, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.assessment_conversation_heading), style = DesignTypography.Title, color = DesignColor.Text)
                Text(stringResource(R.string.placement_introduction_description), style = DesignTypography.Body, color = DesignColor.Muted)
            }
            Column(Modifier.fillMaxWidth().background(DesignColor.SurfaceContainer, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.assessment_estimate_heading), style = DesignTypography.Title, color = DesignColor.Text)
                Text(stringResource(R.string.placement_introduction_disclaimer), style = DesignTypography.Detail, color = DesignColor.Muted)
                Text(stringResource(R.string.placement_no_history), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            Text(stringResource(R.string.promotion_before_start), style = DesignTypography.Title, color = DesignColor.Text)
            PlacementRequirementRow(R.drawable.ic_placement_quiet, stringResource(R.string.placement_quiet_space), stringResource(R.string.assessment_quiet_detail))
            PlacementRequirementRow(R.drawable.ic_placement_microphone, stringResource(R.string.placement_microphone), stringResource(R.string.assessment_microphone_detail))
            PlacementRequirementRow(
                R.drawable.ic_navigation_practice,
                stringResource(R.string.assessment_natural_speech),
                stringResource(R.string.assessment_natural_speech_detail)
            )
            if (hasSaveFailed) Text(stringResource(R.string.placement_save_failed), style = DesignTypography.Detail, color = DesignColor.Error)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.placement_start), onStartAssessment, enabled = !isSaving)
            if (isSaving) com.project.speakmate.ui.components.SpeakMateLoadingIndicator()
            TextButton(onClick = onSkipAssessment, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isSaving) R.string.placement_saving else R.string.placement_later), style = DesignTypography.Button)
            }
        }
    }
}

@Composable
private fun PlacementRequirementRow(@DrawableRes iconRes: Int, text: String, description: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = DesignTypography.Body, color = DesignColor.Text)
            Text(description, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
    }
}
