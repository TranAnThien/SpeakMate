package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationGrammarUiState

@Composable
fun PreparationGrammarCard(
    grammar: PreparationGrammarUiState,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(grammar.title, style = DesignTypography.Title, color = DesignColor.Text)
        Text(grammar.explanation, style = DesignTypography.Detail, color = DesignColor.Muted)
        Column(Modifier.fillMaxWidth().background(DesignColor.ExampleSurface, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.preparation_structure), style = DesignTypography.Label, color = DesignColor.Muted)
            Text(grammar.structure, style = DesignTypography.Detail, color = DesignColor.Text)
        }
        PreparationExampleCard(grammar.example, onPlayExample)
        PreparationNote(grammar.usageNote)
    }
}
