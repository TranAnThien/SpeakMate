package com.project.speakmate.ui.components.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun AssessmentPendingContent(
    onBack: () -> Unit,
    onRetryGrading: () -> Unit,
    onSaveUnassessed: (() -> Unit)?,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(title, onBack)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SpeakMateCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                StatusContent(
                    title =
                        stringResource(
                            if (onSaveUnassessed == null) R.string.assessment_retry_title
                            else R.string.assessment_pending_heading
                        ),
                    titleStyle = DesignTypography.Heading,
                    titleColor = DesignColor.Text,
                    titleTextAlign = if (onSaveUnassessed == null) TextAlign.Center else TextAlign.Start,
                    description =
                        stringResource(
                            if (onSaveUnassessed == null) R.string.assessment_retry_description
                            else R.string.assessment_pending_description
                        ),
                    descriptionStyle = DesignTypography.Body,
                    descriptionColor = DesignColor.Muted,
                    descriptionTextAlign = if (onSaveUnassessed == null) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment =
                        if (onSaveUnassessed == null) Alignment.CenterHorizontally else Alignment.Start,
                    illustration = {
                        if (onSaveUnassessed == null) {
                            AssessmentRetryMascot(Modifier.size(160.dp))
                        } else
                            CaptionText(
                                text = stringResource(R.string.assessment_pending_title),
                                style = DesignTypography.Badge,
                                color = DesignColor.Primary
                            )
                    }
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.assessment_retry_grading), onRetryGrading)
            if (onSaveUnassessed != null)
                SecondaryButton(stringResource(R.string.assessment_save_unassessed), onSaveUnassessed)
        }
    }
}

@Composable
private fun AssessmentRetryMascot(modifier: Modifier = Modifier) {
    AnimatedMascot(animation = MascotAnimation.NETWORK_ERROR, modifier = modifier)
}
