package com.project.speakmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun LoadErrorDialog(
    title: String,
    description: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = DesignTypography.SectionTitle,
    descriptionStyle: TextStyle = DesignTypography.Detail,
    actionMinHeight: Dp = 48.dp,
    actionSpacing: Dp = 16.dp
) {
    FullScreenDialog(onDismissRequest = onRetry, modifier = modifier) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.widthIn(max = 380.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = DesignColor.SurfaceContainerHigh,
                shadowElevation = 4.dp
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(48.dp).background(DesignColor.PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_load_error), null, Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = titleStyle, color = DesignColor.Text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(description, style = descriptionStyle, color = DesignColor.Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(actionSpacing))
                    PrimaryButton(stringResource(R.string.content_retry), onRetry, minHeight = actionMinHeight)
                }
            }
        }
    }
}
