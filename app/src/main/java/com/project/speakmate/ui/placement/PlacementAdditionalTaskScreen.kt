package com.project.speakmate.ui.placement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

@Composable
fun PlacementAdditionalTaskScreen(
    onBack: () -> Unit,
    onContinueAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.RoutingBackground)) {
        PlacementAppBar(stringResource(R.string.placement_title), onBack)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
                .padding(horizontal = 32.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 348.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).background(DesignColor.PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_placement_assignment), null, Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.width(142.dp).background(DesignColor.PaleBlue, RoundedCornerShape(100.dp))
                        .padding(6.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.placement_unconfirmed), style = DesignTypography.Label, color = DesignColor.Primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.placement_confirmation_title), style = DesignTypography.Heading,
                        color = DesignColor.Primary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.placement_additional_description), style = DesignTypography.Body,
                        color = DesignColor.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 14.dp))
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.placement_next_step), style = DesignTypography.Label, color = DesignColor.Muted)
                        Text(stringResource(R.string.placement_additional_task), style = DesignTypography.Title, color = DesignColor.Text)
                        Text(stringResource(R.string.placement_additional_task_description), style = DesignTypography.Detail, color = DesignColor.Muted)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth().background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_placement_info), null, Modifier.size(20.dp, 24.dp))
                        Text(stringResource(R.string.placement_level_unchanged), style = DesignTypography.Notice, color = DesignColor.Primary)
                    }
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(stringResource(R.string.placement_continue), onContinueAssessment)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.placement_confirmation_note), style = DesignTypography.Caption,
                        color = DesignColor.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}

@Preview(name = "Placement / Additional same-level task / 35:3693", widthDp = 412, heightDp = 835, showBackground = true)
@Composable
private fun PlacementAdditionalTaskScreenPreview() {
    SpeakMateTheme {
        PlacementAdditionalTaskScreen({}, {})
    }
}
