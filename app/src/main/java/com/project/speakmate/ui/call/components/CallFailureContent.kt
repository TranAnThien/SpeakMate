package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallFailureContent(
    description: String,
    isConnectionFailure: Boolean,
    retryLabel: String?,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        CallPageHeader(
            stringResource(if (isConnectionFailure) R.string.call_failure_header else R.string.call_failure_general_header),
            onExit,
            isClose = true
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
                .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 36.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Image(painterResource(R.drawable.img_call_error), null, Modifier.size(200.dp))
                    Text(stringResource(R.string.call_failure_title), style = DesignTypography.Heading, color = DesignColor.Primary, textAlign = TextAlign.Center)
                    Text(description, style = DesignTypography.Body, color = DesignColor.Text, textAlign = TextAlign.Center)
                    if (isConnectionFailure) {
                        Text(stringResource(R.string.call_failure_note), style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (retryLabel != null) PrimaryButton(retryLabel, onRetry)
            CallSecondaryButton(stringResource(R.string.call_exit), onExit)
        }
    }
}
