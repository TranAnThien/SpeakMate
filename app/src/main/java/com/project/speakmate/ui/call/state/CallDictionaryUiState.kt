package com.project.speakmate.ui.call.state

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class CallDictionaryUiState(
    val word: String = "",
    val pronunciationLabel: String = "",
    val meaning: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val messageId: String = "",
    val selectionStart: Int = 0,
    val anchorTopDp: Float = 0f
)
