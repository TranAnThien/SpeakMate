package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationExampleUiState

@Composable
fun PreparationExampleCard(
    example: PreparationExampleUiState,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(example.sentence, style = DesignTypography.Detail, color = DesignColor.Text)
        Text(example.translation, style = DesignTypography.Detail, color = DesignColor.Muted)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.preparation_listen_example), style = DesignTypography.Detail, color = DesignColor.Primary,
                modifier = Modifier.weight(1f).clickable(onClick = onPlayExample).padding(vertical = 14.dp))
            PreparationAudioButton(onPlayExample)
        }
    }
}
