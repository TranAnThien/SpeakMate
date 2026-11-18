package com.project.speakmate.domain.model

sealed interface PracticeCompletionState {
    data object Unavailable : PracticeCompletionState
    data object Grading : PracticeCompletionState
    data class Saved(val completion: PracticeCompletion) : PracticeCompletionState
    data class SaveFailed(
        val completion: PracticeCompletion,
        val isSaving: Boolean = false
    ) : PracticeCompletionState
}
