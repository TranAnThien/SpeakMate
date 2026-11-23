package com.project.speakmate.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.StateIllustration
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeUnavailableContent(hasDialog: Boolean, onRetry: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val contentHeight = maxHeight
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
            .padding(horizontal = 40.dp).padding(top = 24.dp, bottom = 48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StateIllustration(R.drawable.ic_home_dashboard)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(if (hasDialog) R.string.home_empty_title else R.string.home_error_title),
                    style = DesignTypography.SectionTitle, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(if (hasDialog) R.string.home_empty_description else R.string.home_error_description),
                    style = DesignTypography.EmptyBody, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(52.dp))
                if (!hasDialog) PrimaryButton(stringResource(R.string.home_retry), onRetry)
            }
        }
    }
}
