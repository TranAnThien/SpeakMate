package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText

@Composable
fun TrainingPathRefreshNotice(isRefreshing: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TitleText(
            text =
                stringResource(
                    if (isRefreshing) R.string.training_refreshing
                    else R.string.training_stale_title
                )
        )
        DetailText(text = stringResource(R.string.training_stale_description))
        if (isRefreshing) SpeakMateLoadingIndicator(size = 24.dp)
    }
}
