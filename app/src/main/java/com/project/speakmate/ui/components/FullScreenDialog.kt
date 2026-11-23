package com.project.speakmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.project.speakmate.app.hideSpeakMateSystemBars
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            if (window != null) {
                window.setDimAmount(0f)
                hideSpeakMateSystemBars(window)
            }
        }
        Surface(modifier.fillMaxSize(), color = DesignColor.Scrim) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent.union(WindowInsets(top = 32.dp, bottom = 24.dp)))) {
                content()
            }
        }
    }
}
