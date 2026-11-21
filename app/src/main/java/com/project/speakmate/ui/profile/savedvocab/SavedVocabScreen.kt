package com.project.speakmate.ui.profile.savedvocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.ui.components.ContentStatusPanel
import com.project.speakmate.ui.profile.savedvocab.components.VocabularyCard
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedVocabScreen(
    uiState: SavedVocabUiState,
    onNavigateBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onPlayClick: (VocabularyItem) -> Unit,
    onDeleteClick: (VocabularyItem) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_vocab_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.saved_vocab_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.saved_vocab_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.saved_vocab_clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.hasDeleteError) {
                Text(stringResource(R.string.saved_vocab_delete_error), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
            }
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.hasLoadError) {
                ContentStatusPanel(
                    R.drawable.ic_load_error, stringResource(R.string.saved_vocab_title),
                    stringResource(R.string.saved_vocab_load_error), stringResource(R.string.content_retry),
                    stringResource(R.string.navigation_profile), onRetry, onNavigateBack
                )
            } else if (uiState.isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.saved_vocab_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = uiState.filteredVocabularies.distinctBy { it.word to it.partOfSpeech },
                        key = { _, item -> "${item.word.length}:${item.word}${item.partOfSpeech}" },
                        contentType = { _, _ -> VocabularyItem::class }
                    ) { _, vocabItem ->
                        VocabularyCard(
                            item = vocabItem,
                            onPlayClick = onPlayClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}
