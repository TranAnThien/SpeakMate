package com.project.speakmate.ui.components.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ContentPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 18.dp)
) {
    Box(modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .padding(contentPadding)) {
        HeadingText(text = title, style = DesignTypography.PageTitle, color = DesignColor.Primary)
    }
}
