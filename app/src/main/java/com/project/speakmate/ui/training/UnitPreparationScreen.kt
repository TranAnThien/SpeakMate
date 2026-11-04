package com.project.speakmate.ui.training

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.PreparationGrammarCard
import com.project.speakmate.ui.training.components.PreparationHeader
import com.project.speakmate.ui.training.components.PreparationNote
import com.project.speakmate.ui.training.components.PreparationOverviewContent
import com.project.speakmate.ui.training.components.PreparationPatternCard
import com.project.speakmate.ui.training.components.PreparationTabs
import com.project.speakmate.ui.training.components.PreparationVocabularyCard
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.UnitPreparationUiState

@Composable
fun UnitPreparationScreen(
    state: UnitPreparationUiState,
    onBack: () -> Unit,
    onSelectSection: (PreparationSection) -> Unit,
    onStartRoleplay: () -> Unit,
    onPlayWord: (String) -> Unit,
    onPlayPattern: (String) -> Unit,
    onPlayExample: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    val onOpenGrammar = remember(onSelectSection) { { onSelectSection(PreparationSection.GRAMMAR) } }
    val onOpenPatterns = remember(onSelectSection) { { onSelectSection(PreparationSection.SENTENCE_PATTERNS) } }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PreparationHeader(stringResource(when (state.section) {
            PreparationSection.OVERVIEW -> R.string.preparation_title
            PreparationSection.VOCABULARY -> R.string.preparation_vocabulary
            PreparationSection.GRAMMAR -> R.string.preparation_grammar
            PreparationSection.SENTENCE_PATTERNS -> R.string.preparation_patterns
        }), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().drawWithContent {
            drawContent()
            val scrollRange = scrollState.maxValue
            if (scrollRange > 0 && scrollRange != Int.MAX_VALUE) {
                val trackHeight = (size.height - 24.dp.toPx()).coerceAtLeast(0f)
                val contentHeight = (size.height + scrollRange - 36.dp.toPx()).coerceAtLeast(size.height)
                if (contentHeight > 0f) {
                    val thumbHeight = (trackHeight * size.height / contentHeight).coerceAtMost(trackHeight)
                    val thumbTop = 12.dp.toPx() + (trackHeight - thumbHeight) * scrollState.value.toFloat() / scrollRange
                    drawRoundRect(DesignColor.ScrollIndicator, Offset(size.width - 7.dp.toPx(), thumbTop),
                        Size(3.dp.toPx(), thumbHeight), CornerRadius(2.dp.toPx()))
                }
            }
        }.verticalScroll(scrollState).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(state.contextLabel, style = DesignTypography.Label, color = DesignColor.Muted)
            Text(state.title, style = DesignTypography.PageTitle, color = DesignColor.Primary)
            Text(state.description, style = DesignTypography.Detail, color = DesignColor.Muted)
            if (state.section != PreparationSection.OVERVIEW) PreparationTabs(state.section, onSelectSection)
            when (state.section) {
                PreparationSection.OVERVIEW -> PreparationOverviewContent(state.overview, onSelectSection)
                PreparationSection.VOCABULARY -> {
                    PreparationNote(stringResource(R.string.preparation_audio_note))
                    state.vocabulary.forEach { word ->
                        key(word.id) {
                            val onPlay = remember(word.id, onPlayWord) { { onPlayWord(word.id) } }
                            val onExample = remember(word.id, onPlayExample) { { onPlayExample(word.id) } }
                            PreparationVocabularyCard(word, onPlay, onExample)
                        }
                    }
                }
                PreparationSection.GRAMMAR -> {
                    state.grammar.forEach { grammar ->
                        key(grammar.id) {
                            val onExample = remember(grammar.id, onPlayExample) { { onPlayExample(grammar.id) } }
                            PreparationGrammarCard(grammar, onExample)
                        }
                    }
                    PreparationNote(stringResource(R.string.preparation_grammar_note))
                }
                PreparationSection.SENTENCE_PATTERNS -> {
                    state.sentencePatterns.forEach { pattern ->
                        key(pattern.id) {
                            val onPlay = remember(pattern.id, onPlayPattern) { { onPlayPattern(pattern.id) } }
                            val onExample = remember(pattern.id, onPlayExample) { { onPlayExample(pattern.id) } }
                            PreparationPatternCard(pattern, onPlay, onExample)
                        }
                    }
                    PreparationNote(stringResource(R.string.preparation_patterns_note))
                }
            }
        }
        Box(Modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp, vertical = 16.dp)) {
            PrimaryButton(stringResource(when (state.section) {
                PreparationSection.VOCABULARY -> R.string.preparation_next_grammar
                PreparationSection.GRAMMAR -> R.string.preparation_next_patterns
                else -> R.string.preparation_start_roleplay
            }), when (state.section) {
                PreparationSection.VOCABULARY -> onOpenGrammar
                PreparationSection.GRAMMAR -> onOpenPatterns
                else -> onStartRoleplay
            })
        }
    }
}
