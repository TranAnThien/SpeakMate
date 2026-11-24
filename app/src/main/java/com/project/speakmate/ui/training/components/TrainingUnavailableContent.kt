package com.project.speakmate.ui.training.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingUnavailableContent(
    isLoading: Boolean,
    onRetry: () -> Unit,
    errorMessage: String = "",
    @StringRes loadingTitleRes: Int = R.string.training_loading_title,
    @StringRes loadingDescriptionRes: Int = R.string.training_loading_description,
    @StringRes errorTitleRes: Int = R.string.training_error_title,
    @StringRes errorDescriptionRes: Int = R.string.training_error_description,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val contentHeight = maxHeight
        Box(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = contentHeight)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StatusContent(
                title = stringResource(if (isLoading) loadingTitleRes else errorTitleRes),
                titleStyle = DesignTypography.Heading,
                titleColor = DesignColor.Text,
                titleTextAlign = TextAlign.Center,
                description =
                    if (isLoading) stringResource(loadingDescriptionRes)
                    else errorMessage.ifBlank { stringResource(errorDescriptionRes) },
                descriptionStyle = DesignTypography.Body,
                descriptionColor = DesignColor.Muted,
                descriptionTextAlign = TextAlign.Center,
                descriptionModifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                illustration = {
                    if (isLoading) {
                        Box(
                            Modifier.size(64.dp).background(DesignColor.PrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SpeakMateLoadingIndicator(size = 32.dp)
                        }
                    } else
                        AnimatedMascot(animation = MascotAnimation.NETWORK_ERROR, modifier = Modifier.size(140.dp))
                    Spacer(Modifier.height(16.dp))
                },
                titleContent = { Spacer(Modifier.height(16.dp)) },
                actions = {
                    if (!isLoading) {
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = stringResource(R.string.content_retry),
                            onClick = onRetry,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            )
        }
    }
}
