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
    titleStyle: TextStyle = DesignTypography.Heading,
    descriptionStyle: TextStyle = DesignTypography.Body,
    actionMinHeight: Dp = 56.dp,
    actionSpacing: Dp = 12.dp
) {
    Surface(modifier.fillMaxSize(), color = DesignColor.Scrim) {
        Box(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 348.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .background(DesignColor.Surface, RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).background(DesignColor.PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_load_error), null, Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(title, style = titleStyle, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(description, style = descriptionStyle, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(actionSpacing))
                PrimaryButton(stringResource(R.string.content_retry), onRetry, minHeight = actionMinHeight)
            }
        }
    }
}
