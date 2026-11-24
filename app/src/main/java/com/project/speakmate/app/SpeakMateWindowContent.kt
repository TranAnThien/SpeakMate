package com.project.speakmate.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun SpeakMateWindowContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxSize(), color = DesignColor.Background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeContent.union(WindowInsets(top = 32.dp, bottom = 24.dp)))) {
            content()
        }
    }
}
