package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.utils.extension.default

@Composable
fun ContentErrorDialog(title: String, description: String, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, primaryText: String? = null) {
    MessageDialog(title, description, onDismiss, modifier,
        icon = {
            Box(Modifier.size(64.dp).background(DesignColor.SecondaryButton, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_load_error), null, Modifier.size(32.dp), tint = DesignColor.Primary)
            }
        }
    ) {
        DialogActions(primaryText.default(stringResource(R.string.content_reload)),
            stringResource(R.string.content_dismiss_notice), onRetry, onDismiss)
    }
}
