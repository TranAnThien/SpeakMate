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
import com.project.speakmate.domain.model.PronunciationPlaybackState
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.PreparationGrammarCard
import com.project.speakmate.ui.training.components.PreparationNote
import com.project.speakmate.ui.training.components.PreparationOverviewContent
import com.project.speakmate.ui.training.components.PreparationPatternCard
import com.project.speakmate.ui.training.components.PreparationTabs
import com.project.speakmate.ui.training.components.PreparationVocabularyCard
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.UnitPreparationUiState
import com.project.speakmate.utils.extension.default

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
    scrollState: ScrollState? = null,
    noticeContent: @Composable () -> Unit = {}
) {
    val idlePlayback = remember { PronunciationPlaybackState() }
    val sectionScrolls = PreparationSection.entries.map { rememberScrollState() }
    val activeScroll = scrollState.default(sectionScrolls[state.section.ordinal])

    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.preparation_title), onBack)
        PreparationTabs(state.section, onSelectSection)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val scrollRange = activeScroll.maxValue
                    if (scrollRange > 0 && scrollRange != Int.MAX_VALUE) {
                        val trackHeight = (size.height - 24.dp.toPx()).coerceAtLeast(0f)
                        val contentHeight = (size.height + scrollRange - 36.dp.toPx()).coerceAtLeast(size.height)
                        if (contentHeight > 0f) {
                            val thumbHeight = (trackHeight * size.height / contentHeight).coerceAtMost(trackHeight)
                            val thumbTop =
                                12.dp.toPx() + (trackHeight - thumbHeight) * activeScroll.value.toFloat() / scrollRange
                            drawRoundRect(
                                color = DesignColor.ScrollIndicator,
                                topLeft = Offset(size.width - 7.dp.toPx(), thumbTop),
                                size = Size(3.dp.toPx(), thumbHeight),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
                .verticalScroll(activeScroll)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CaptionText(text = stringResource(R.string.preparation_unit_context), style = DesignTypography.Label)
            HeadingText(text = state.title, style = DesignTypography.PageTitle, color = DesignColor.Primary)
            if (state.section != PreparationSection.OVERVIEW) {
                DetailText(text = state.description)
            }

            when (state.section) {
                PreparationSection.OVERVIEW ->
                    PreparationOverviewContent(
                        overview = state.overview,
                        context = state.description,
                        onSelectSection = onSelectSection
                    )
                PreparationSection.VOCABULARY -> {
                    PreparationNote(stringResource(R.string.preparation_audio_note))
                    if (state.vocabulary.isEmpty())
                        PreparationNote(stringResource(R.string.preparation_empty_vocabulary))
                    state.vocabulary.forEach { word ->
                        key(word.id) {
                            val onPlay = remember(word.word, onPlayWord) { { onPlayWord(word.word) } }
                            val onExample =
                                remember(word.example.sentence, onPlayExample) {
                                    { onPlayExample(word.example.sentence) }
                                }
                            PreparationVocabularyCard(
                                word = word,
                                onPlayWord = onPlay,
                                onPlayExample = onExample,
                                playback =
                                    if (state.playback.text == word.word ||
                                        state.playback.text == word.example.sentence) {
                                        state.playback
                                    } else idlePlayback
                            )
                        }
                    }
                }
                PreparationSection.GRAMMAR -> {
                    if (state.grammar.isEmpty()) PreparationNote(stringResource(R.string.preparation_empty_grammar))
                    state.grammar.forEach { grammar ->
                        key(grammar.id) {
                            PreparationGrammarCard(
                                grammar = grammar,
                                onPlayExample = onPlayExample,
                                playback =
                                    if (grammar.examples.any { it.sentence == state.playback.text }) {
                                        state.playback
                                    } else idlePlayback
                            )
                        }
                    }
                    PreparationNote(stringResource(R.string.preparation_grammar_note))
                }
                PreparationSection.SENTENCE_PATTERNS -> {
                    if (state.sentencePatterns.isEmpty())
                        PreparationNote(stringResource(R.string.preparation_empty_patterns))
                    state.sentencePatterns.forEach { pattern ->
                        key(pattern.id) {
                            val onPlay =
                                remember(pattern.pattern, onPlayPattern) { { onPlayPattern(pattern.pattern) } }
                            PreparationPatternCard(
                                pattern = pattern,
                                onPlayPattern = onPlay,
                                onPlayExample = onPlayExample,
                                playback =
                                    if (state.playback.text == pattern.pattern ||
                                        pattern.examples.any { it.sentence == state.playback.text }) {
                                        state.playback
                                    } else idlePlayback
                            )
                        }
                    }
                    PreparationNote(stringResource(R.string.preparation_patterns_note))
                }
            }
        }
        noticeContent()
        Box(Modifier
            .fillMaxWidth()
            .background(DesignColor.Surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)) {
            PrimaryButton(
                text =
                    stringResource(
                        if (state.isStarting) R.string.preparation_start_checking
                        else R.string.preparation_start_roleplay
                    ),
                onClick = onStartRoleplay,
                enabled = state.canStartRoleplay && !state.isStarting && !state.isLocked
            )
        }
    }
}
