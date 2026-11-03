package com.project.speakmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ContentPageHeader(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp).padding(top = 7.dp)) {
        Text(title, style = DesignTypography.PageTitle, color = DesignColor.Primary)
    }
}
