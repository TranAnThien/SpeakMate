package com.project.speakmate.ui.placement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

@Composable
fun PlacementResumeDialog(onResume: () -> Unit, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = DesignColor.Scrim) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 364.dp).fillMaxWidth().offset(y = (-16).dp),
                shape = RoundedCornerShape(24.dp), color = DesignColor.Surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).background(DesignColor.IconBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_placement_pending), null, Modifier.size(32.dp), tint = DesignColor.Primary)
                    }
                    Text(stringResource(R.string.placement_resume_title), style = DesignTypography.Heading,
                        color = DesignColor.Text, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.placement_resume_description), style = DesignTypography.Body,
                        color = DesignColor.Muted, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(stringResource(R.string.placement_restart), onRestart, Modifier.weight(1f))
                        PrimaryButton(stringResource(R.string.placement_resume), onResume, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlacementResumeDialogPreview() {
    SpeakMateTheme {
        PlacementResumeDialog({}, {})
    }
}
