package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPathLevelInformationBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = DesignColor.SurfaceContainerLow,
        contentWindowInsets = { WindowInsets.safeContent.union(WindowInsets(top = 32.dp, bottom = 24.dp)) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeadingText(
                text = stringResource(R.string.training_estimated_level),
                style = DesignTypography.AppBar
            )
            BodyText(
                text = stringResource(R.string.training_level_information),
                color = DesignColor.Muted
            )
            PrimaryButton(stringResource(R.string.training_understood), onDismiss)
        }
    }
}
