package com.project.speakmate.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun MessageDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    shouldDismissOnClickOutside: Boolean = false,
    actions: @Composable () -> Unit
) {
    FullScreenDialog(onDismiss, modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shouldDismissOnClickOutside) {
                Box(Modifier.matchParentSize().pointerInput(onDismiss) {
                    detectTapGestures { onDismiss() }
                })
            }
            Surface(
                Modifier.padding(horizontal = 16.dp, vertical = 24.dp).widthIn(max = 380.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = DesignColor.SurfaceContainerHigh,
                shadowElevation = 4.dp
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (icon != null) {
                        icon()
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(title, style = DesignTypography.SectionTitle, color = DesignColor.Text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(description, style = DesignTypography.Detail, color = DesignColor.Muted, textAlign = TextAlign.Center)
                    if (content != null) {
                        Spacer(Modifier.height(16.dp))
                        content()
                    }
                    Spacer(Modifier.height(16.dp))
                    actions()
                }
            }
        }
    }
}
