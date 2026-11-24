package com.project.speakmate.ui.call.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallExitNoticeCard(message: String, modifier: Modifier = Modifier) {
    Snackbar(modifier.padding(horizontal = 24.dp), containerColor = DesignColor.Text, contentColor = DesignColor.OnPrimary) {
        Text(message, style = DesignTypography.Body)
    }
}
