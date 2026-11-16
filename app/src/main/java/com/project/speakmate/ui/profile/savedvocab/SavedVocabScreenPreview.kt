package com.project.speakmate.ui.profile.savedvocab

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.ui.profile.savedvocab.state.SavedVocabUiState

@Preview(showBackground = true, name = "Danh sách từ vựng")
@Composable
fun SavedVocabScreenPreview_Success() {
    val fakeData = listOf(
        VocabularyItem(
            word = "resilience",
            mean = "khả năng phục hồi, sự kiên cường",
            phonetic = "/rɪˈzɪl.jəns/",
            partOfSpeech = "noun",
            example = LearningExample(
                sentence = "Trauma therapy helps build resilience.",
                translate = "Liệu pháp điều trị chấn thương giúp xây dựng sự kiên cường."
            )
        ),
        VocabularyItem(
            word = "ubiquitous",
            mean = "có mặt ở khắp nơi",
            phonetic = "/juːˈbɪk.wɪ.təs/",
            partOfSpeech = "adjective",
            example = LearningExample(
                sentence = "Smartphones have become ubiquitous in modern society.",
                translate = "Điện thoại thông minh đã trở nên phổ biến khắp nơi trong xã hội hiện đại."
            )
        ),
        VocabularyItem(
            word = "implement",
            mean = "triển khai, thực hiện",
            phonetic = "/ˈɪm.plɪ.ment/",
            partOfSpeech = "verb",
            example = LearningExample(
                sentence = "We need to implement the new system by next week.",
                translate = "Chúng ta cần triển khai hệ thống mới vào tuần tới."
            )
        )
    )

    MaterialTheme {
        Surface {
            SavedVocabScreenContent(
                uiState = SavedVocabUiState(
                    isLoading = false,
                    searchQuery = "",
                    allVocabularies = fakeData,
                    filteredVocabularies = fakeData
                ),
                onNavigateBack = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onPlayClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Trạng thái Loading")
@Composable
fun SavedVocabScreenPreview_Loading() {
    MaterialTheme {
        Surface {
            SavedVocabScreenContent(
                uiState = SavedVocabUiState(isLoading = true),
                onNavigateBack = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onPlayClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Trạng thái Trống (Empty)")
@Composable
fun SavedVocabScreenPreview_Empty() {
    MaterialTheme {
        Surface {
            SavedVocabScreenContent(
                uiState = SavedVocabUiState(
                    isLoading = false,
                    filteredVocabularies = emptyList()
                ),
                onNavigateBack = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onPlayClick = {},
                onDeleteClick = {}
            )
        }
    }
}