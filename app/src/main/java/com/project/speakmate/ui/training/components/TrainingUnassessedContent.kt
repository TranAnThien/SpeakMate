package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun TrainingUnassessedContent(onStartPlacement: () -> Unit, onFreePractice: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SpeakMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CaptionText(text = stringResource(R.string.training_your_path), style = DesignTypography.Badge)
            HeadingText(text = stringResource(R.string.training_unassessed_title), style = DesignTypography.AppBar)
            TrainingPill(
                text = stringResource(R.string.training_unassessed_badge),
                backgroundColor = DesignColor.SecondaryContainer
            )
            CaptionText(text = stringResource(R.string.training_unassessed_description))
            PrimaryButton(stringResource(R.string.training_start_placement), onStartPlacement)
        }
        OutlinedCard(onClick = onFreePractice, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TitleText(text = stringResource(R.string.navigation_practice))
                DetailText(text = stringResource(R.string.training_unassessed_practice))
            }
        }
    }
}
