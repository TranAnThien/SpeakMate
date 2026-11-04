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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.placement.components.PlacementAppBar
import com.project.speakmate.ui.placement.components.PlacementSecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

@Composable
fun PlacementIntroductionScreen(
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onSkipAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PlacementAppBar("", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(180.dp).border(2.dp, DesignColor.MascotBorder, CircleShape),
                contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.img_placement_mascot), null, Modifier.size(140.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.placement_introduction_title), style = DesignTypography.Heading,
                color = DesignColor.Primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.placement_introduction_description), style = DesignTypography.Body,
                color = DesignColor.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlacementRequirementRow(R.drawable.ic_placement_schedule, stringResource(R.string.placement_duration))
                PlacementRequirementRow(R.drawable.ic_placement_quiet, stringResource(R.string.placement_quiet_space))
                PlacementRequirementRow(R.drawable.ic_placement_microphone, stringResource(R.string.placement_microphone))
                PlacementRequirementRow(R.drawable.ic_placement_history, stringResource(R.string.placement_no_history))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.placement_introduction_disclaimer), style = DesignTypography.Caption,
                color = DesignColor.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.placement_start), onStartAssessment)
            PlacementSecondaryButton(stringResource(R.string.placement_later), onSkipAssessment, background = DesignColor.Background)
        }
    }
}

@Composable
private fun PlacementRequirementRow(@DrawableRes iconRes: Int, text: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(DesignColor.Surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Image(painterResource(iconRes), null, Modifier.size(24.dp))
        }
        Text(text, style = DesignTypography.Detail, color = DesignColor.Text, modifier = Modifier.weight(1f))
    }
}

@Preview(name = "Placement / Introduction / 17:55", widthDp = 396, heightDp = 819, showBackground = true)
@Composable
private fun PlacementIntroductionScreenPreview() {
    SpeakMateTheme {
        PlacementIntroductionScreen({}, {}, {})
    }
}
