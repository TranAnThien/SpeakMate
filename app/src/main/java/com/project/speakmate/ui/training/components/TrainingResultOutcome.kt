package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.project.speakmate.R
import com.project.speakmate.ui.components.AnimatedMascot
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingResultOutcome(
    title: String,
    description: String,
    surface: Color,
    isProcessing: Boolean,
    isPassed: Boolean,
    modifier: Modifier = Modifier,
    isIntroduction: Boolean = false
) {
    Column(modifier.fillMaxWidth().background(surface, RoundedCornerShape(24.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (isIntroduction) Icon(painterResource(R.drawable.ic_training_assessment_coffee), null, Modifier.size(56.dp), tint = DesignColor.Primary)
        else AnimatedMascot(
            animationRes = when {
                isProcessing -> R.raw.lottie_assessing
                isPassed -> R.raw.lottie_result_pass
                else -> R.raw.lottie_result_encourage
            },
            fallbackRes = if (isProcessing) R.drawable.img_call_processing else R.drawable.img_call_listening,
            modifier = Modifier.size(if (isProcessing) 160.dp else 120.dp),
            iterations = if (isProcessing) LottieConstants.IterateForever else 1
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
    }
}
