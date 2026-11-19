package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakePracticeCompletionRepository : PracticeCompletionRepository {
    val state = MutableStateFlow<PracticeCompletionState>(PracticeCompletionState.Grading)
    val retriedIds = mutableListOf<String>()
    val releasedIds = mutableListOf<String>()

    override fun completePractice(practice: CompletedPractice): String? = null

    override fun observeCompletion(attemptId: String) = state

    override fun retrySave(attemptId: String) {
        retriedIds += attemptId
        val current = state.value as? PracticeCompletionState.SaveFailed ?: return
        state.value = current.copy(isSaving = true)
    }

    override fun releaseCompletion(attemptId: String) {
        releasedIds += attemptId
    }
}
