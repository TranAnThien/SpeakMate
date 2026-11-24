package com.project.speakmate.ui.profile.savedvocab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.PronunciationState
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.profile.savedvocab.components.VocabularyCard
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SavedVocabScreen(
    uiState: SavedVocabUiState,
    onNavigateBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onPlayClick: (VocabularyItem) -> Unit,
    onDeleteClick: (VocabularyItem) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onRetryDelete: () -> Unit = {},
    onDismissDeleteError: () -> Unit = {},
    onStartPractice: () -> Unit = {}
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val items = uiState.filteredVocabularies
    val isGlobalEmpty =
        !uiState.isLoading &&
            !uiState.hasLoadError &&
            uiState.allVocabularies.isEmpty() &&
            uiState.searchQuery.isBlank() &&
            uiState.deletingCount == 0
    Column(modifier
        .fillMaxSize()
        .background(DesignColor.Background)
        .imePadding()) {
        BackAppBar(stringResource(R.string.saved_vocab_title), onNavigateBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isGlobalEmpty)
                item(key = "search", contentType = "search") {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.saved_vocab_search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = DesignColor.Muted
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty())
                                IconButton(onClick = onClearSearch) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.saved_vocab_clear_search),
                                        tint = DesignColor.Muted
                                    )
                                }
                        },
                        textStyle = DesignTypography.Body,
                        shape = CircleShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                            KeyboardActions(
                                onSearch = {
                                    keyboard?.hide()
                                    focus.clearFocus()
                                }
                            ),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = DesignColor.SurfaceContainerHigh,
                                unfocusedContainerColor = DesignColor.SurfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                    )
                }
            if (!isGlobalEmpty && (uiState.hasLoaded || items.isNotEmpty()))
                item(key = "count", contentType = "count") {
                    DetailText(
                        text =
                            if (uiState.searchQuery.isBlank())
                                pluralStringResource(
                                    R.plurals.saved_vocab_total_words,
                                    uiState.visibleCount,
                                    uiState.visibleCount
                                )
                            else
                                pluralStringResource(
                                    R.plurals.saved_vocab_matching_words,
                                    items.size,
                                    items.size,
                                    uiState.searchQuery
                                )
                    )
                }
            if (uiState.hasDeleteError)
                item(key = "delete-error", contentType = "delete-error") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailText(
                            text = stringResource(R.string.saved_vocab_delete_error),
                            color = DesignColor.Error
                        )
                        if (!uiState.hasLoadError) {
                            SecondaryButton(
                                text = stringResource(R.string.content_retry),
                                onClick = onRetryDelete,
                                enabled = uiState.failedDeletionIdentity != null
                            )
                        }
                        SecondaryButton(stringResource(R.string.content_close), onDismissDeleteError)
                    }
                }
            if (uiState.isLoading) item(key = "loading", contentType = "loading") { SpeakMateLoadingIndicator() }
            if (uiState.hasLoadError)
                item(key = "load-error", contentType = "load-error") {
                    StatusContent(
                        title = "",
                        description =
                            stringResource(
                                if (uiState.hasLoaded) R.string.saved_vocab_stale
                                else R.string.saved_vocab_load_error
                            ),
                        descriptionStyle = DesignTypography.Body,
                        descriptionColor = DesignColor.Muted,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        illustration = {
                            if (!uiState.hasLoaded)
                                AnimatedMascot(
                                    animation = MascotAnimation.NETWORK_ERROR,
                                    modifier = Modifier.size(116.dp),
                                    fallbackRes = R.drawable.ic_load_error
                                )
                        },
                        actions = { PrimaryButton(stringResource(R.string.content_retry), onRetry) }
                    )
                }
            if (!uiState.isLoading && !uiState.hasLoadError && items.isEmpty() && uiState.deletingCount == 0)
                item(key = "empty", contentType = "empty") {
                    StatusContent(
                        title = "",
                        description =
                            stringResource(
                                if (uiState.searchQuery.isNotBlank()) R.string.saved_vocab_no_matches
                                else R.string.saved_vocab_empty
                            ),
                        descriptionStyle = DesignTypography.Body,
                        descriptionColor = DesignColor.Muted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        illustration = {
                            AnimatedMascot(
                                animation = MascotAnimation.EMPTY_STATE,
                                modifier = Modifier.size(140.dp),
                                fallbackRes = R.drawable.ic_practice_empty_catalog
                            )
                        },
                        actions = {
                            if (uiState.searchQuery.isNotBlank())
                                SecondaryButton(stringResource(R.string.saved_vocab_clear_search), onClearSearch)
                            else {
                                BodyText(
                                    text = stringResource(R.string.saved_vocab_empty_description),
                                    color = DesignColor.Muted
                                )
                                PrimaryButton(stringResource(R.string.history_start_practice), onStartPractice)
                            }
                        }
                    )
                }
            items(
                items = items,
                key = {
                    val id = SavedVocabularyRules.getIdentity(it)
                    "${id.word.length}:${id.word}${id.partOfSpeech}"
                },
                contentType = { VocabularyItem::class }
            ) { item ->
                VocabularyCard(
                    item = item,
                    onPlayClick = onPlayClick,
                    onDeleteClick = onDeleteClick,
                    playbackStatus =
                        if (uiState.pronunciationIdentity == SavedVocabularyRules.getIdentity(item) &&
                            uiState.playback.text == item.word)
                            uiState.playback.status
                        else PronunciationState.IDLE
                )
            }
        }
        if (uiState.deletingCount > 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .background(DesignColor.SurfaceContainerHigh)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.deletingCount > 0)
                    DetailText(
                        text =
                            pluralStringResource(
                                R.plurals.saved_vocab_deleting_words,
                                uiState.deletingCount,
                                uiState.deletingCount
                            )
                    )
            }
        }
    }
}
