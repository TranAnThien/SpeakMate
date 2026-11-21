package com.project.speakmate.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeTextAction(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.CenterStart) {
        Text(text, style = DesignTypography.Link, color = DesignColor.Primary)
    }
}
