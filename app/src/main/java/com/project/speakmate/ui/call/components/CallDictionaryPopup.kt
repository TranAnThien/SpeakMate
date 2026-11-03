package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.state.CallDictionaryUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallDictionaryPopup(
    state: CallDictionaryUiState,
    onClose: () -> Unit,
    onPlayPronunciation: () -> Unit,
    onSaveWord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth(), color = DesignColor.Surface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DesignColor.DictionaryBorder)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.word, style = DesignTypography.Title, color = DesignColor.Text)
                    Text(state.pronunciationLabel, style = DesignTypography.Detail, color = DesignColor.Muted)
                }
                Box(Modifier.size(48.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)).clickable(onClick = onPlayPronunciation),
                    contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_training_preparation_speaker), null, Modifier.size(22.dp))
                }
                Box(Modifier.size(48.dp).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_call_close), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Muted))
                }
            }
            Text(stringResource(R.string.call_dictionary_meaning, state.meaning), style = DesignTypography.Detail, color = DesignColor.Text)
            Column(Modifier.fillMaxWidth().background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.exampleSentence, style = DesignTypography.Detail, color = DesignColor.Text)
                Text(state.exampleTranslation, style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            CallSecondaryButton(stringResource(R.string.call_dictionary_save), onSaveWord)
        }
    }
}
