package com.project.speakmate.ui.placement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun AssessmentRecoveryContent(title: String, description: String, onStartAgain: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(title, onBack)
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.assessment_recovery_title), style = DesignTypography.Heading, color = DesignColor.Text)
                Text(description, style = DesignTypography.Body, color = DesignColor.Muted)
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.recovery_start_again), onStartAgain)
            SecondaryButton(stringResource(R.string.content_back), onBack)
        }
    }
}
