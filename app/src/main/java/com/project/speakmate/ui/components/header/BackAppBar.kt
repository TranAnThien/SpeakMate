package com.project.speakmate.ui.components.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun BackAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isClose: Boolean = false,
    isBackEnabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp), enabled = isBackEnabled) {
            Icon(
                painter = painterResource(if (isClose) R.drawable.ic_call_close else R.drawable.ic_placement_back),
                contentDescription =
                    stringResource(if (isClose) R.string.accessibility_close else R.string.accessibility_back),
                modifier = Modifier.size(24.dp),
                tint = DesignColor.Text
            )
        }
        Box(Modifier.weight(1f).padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            HeadingText(text = title, style = DesignTypography.AppBar)
        }
    }
}
