package com.project.speakmate.ui.training.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.speakmate.R
import com.project.speakmate.ui.components.header.BackAppBar

@Composable
fun TrainingUnavailableScreen(
    title: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    @StringRes loadingTitleRes: Int = R.string.training_loading_title,
    @StringRes loadingDescriptionRes: Int = R.string.training_loading_description,
    @StringRes errorTitleRes: Int = R.string.training_error_title,
    @StringRes errorDescriptionRes: Int = R.string.training_error_description
) {
    Column(modifier.fillMaxSize()) {
        BackAppBar(title = title, onBack = onBack)
        TrainingUnavailableContent(
            isLoading = isLoading,
            onRetry = onRetry,
            errorMessage = errorMessage,
            loadingTitleRes = loadingTitleRes,
            loadingDescriptionRes = loadingDescriptionRes,
            errorTitleRes = errorTitleRes,
            errorDescriptionRes = errorDescriptionRes,
            modifier = Modifier.weight(1f)
        )
    }
}
