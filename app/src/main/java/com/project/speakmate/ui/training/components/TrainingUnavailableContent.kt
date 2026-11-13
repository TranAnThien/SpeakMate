package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.StateIllustration
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
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
            .padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    Box(Modifier.size(64.dp).background(DesignColor.PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                        SpeakMateLoadingIndicator(size = 32.dp)
                    }
                } else StateIllustration(R.drawable.ic_training_empty_path)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(if (isLoading) loadingTitleRes else errorTitleRes),
                    style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isLoading) stringResource(loadingDescriptionRes)
                    else errorMessage.ifBlank { stringResource(errorDescriptionRes) },
                    style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp))
                if (!isLoading) {
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(stringResource(R.string.content_retry), onRetry, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
