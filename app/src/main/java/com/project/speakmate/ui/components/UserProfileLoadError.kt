package com.project.speakmate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R

@Composable
fun UserProfileLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ContentStatusPanel(
        iconRes = R.drawable.ic_load_error,
        title = stringResource(R.string.user_profile_load_error_title),
        description = stringResource(R.string.user_profile_load_error_description),
        primaryText = stringResource(R.string.content_reload),
        secondaryText = "",
        onPrimaryAction = onRetry,
        onSecondaryAction = {},
        modifier = modifier
    )
}
