package com.project.speakmate.ui.training.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.image.ScenarioImage
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.PreparationModuleUiState
import com.project.speakmate.ui.training.state.PreparationOverviewUiState
import com.project.speakmate.ui.training.state.PreparationSection

@Composable
fun PreparationOverviewContent(
    overview: PreparationOverviewUiState,
    context: String,
    onSelectSection: (PreparationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ScenarioImage(overview.imageUrl, Modifier.fillMaxWidth().height(180.dp))
        SpeakMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CaptionText(text = stringResource(R.string.preparation_context), style = DesignTypography.Label)
            DetailText(
                text = context.removePrefix(stringResource(R.string.preparation_context_prefix)).trim(),
                color = DesignColor.Text
            )
        }
        SpeakMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CaptionText(
                text = stringResource(R.string.preparation_communicative_focus),
                style = DesignTypography.Label
            )
            DetailText(text = overview.communicativeFocus, color = DesignColor.Text)
        }
        CaptionText(text = stringResource(R.string.preparation_modules_heading), style = DesignTypography.Label)
        overview.modules.forEach { module ->
            key(module.section) {
                val onOpen = remember(module.section, onSelectSection) { { onSelectSection(module.section) } }
                PreparationModuleCard(module, onOpen)
            }
        }
        SpeakMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CaptionText(
                text = stringResource(R.string.preparation_required_actions),
                style = DesignTypography.Label
            )
            overview.requiredActions.forEach { action ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Image(
                        painter = painterResource(R.drawable.ic_training_preparation_requirement),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    DetailText(text = action, color = DesignColor.Text, modifier = Modifier.weight(1f))
                }
            }
        }
        PreparationNote(stringResource(R.string.preparation_overview_note))
    }
}

@Composable
private fun PreparationModuleCard(module: PreparationModuleUiState, onOpen: () -> Unit) {
    SpeakMateCard(onClick = onOpen, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(DesignColor.PrimaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter =
                        painterResource(
                            when (module.section) {
                                PreparationSection.VOCABULARY -> R.drawable.ic_training_preparation_vocabulary
                                PreparationSection.GRAMMAR -> R.drawable.ic_placement_range
                                PreparationSection.SENTENCE_PATTERNS ->
                                    R.drawable.ic_training_preparation_patterns
                                PreparationSection.OVERVIEW -> R.drawable.ic_training_preparation_vocabulary
                            }
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TitleText(
                    text =
                        stringResource(
                            when (module.section) {
                                PreparationSection.VOCABULARY -> R.string.preparation_vocabulary
                                PreparationSection.GRAMMAR -> R.string.preparation_grammar
                                else -> R.string.preparation_patterns
                            }
                        )
                )
                DetailText(
                    text =
                        pluralStringResource(
                            when (module.section) {
                                PreparationSection.VOCABULARY -> R.plurals.preparation_vocabulary_items
                                PreparationSection.GRAMMAR -> R.plurals.preparation_grammar_items
                                else -> R.plurals.preparation_pattern_items
                            },
                            module.itemCount,
                            module.itemCount
                        )
                )
            }
        }
        DetailText(text = stringResource(R.string.preparation_open_content), color = DesignColor.Primary)
    }
}
