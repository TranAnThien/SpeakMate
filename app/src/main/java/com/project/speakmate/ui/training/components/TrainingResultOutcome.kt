package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
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
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.StatusContent
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
    StatusContent(
        title = title,
        titleStyle = DesignTypography.Heading,
        titleColor = DesignColor.Text,
        titleTextAlign = TextAlign.Center,
        description = description,
        descriptionStyle = DesignTypography.Body,
        descriptionColor = DesignColor.Muted,
        descriptionTextAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        illustration = {
            if (isIntroduction)
                Icon(
                    painter = painterResource(R.drawable.ic_training_assessment_coffee),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = DesignColor.Primary
                )
            else
                AnimatedMascot(
                    animation =
                        when {
                            isProcessing -> MascotAnimation.ASSESSING
                            isPassed -> MascotAnimation.RESULT_PASS
                            else -> MascotAnimation.RESULT_ENCOURAGE
                        },
                    modifier = Modifier.size(if (isProcessing) 160.dp else 120.dp),
                    fallbackRes = if (isProcessing) R.drawable.img_call_processing else R.drawable.img_call_listening,
                    iterations = if (isProcessing) LottieConstants.IterateForever else 1
                )
            Spacer(Modifier.height(12.dp))
        },
        titleContent = { Spacer(Modifier.height(12.dp)) }
    )
}
