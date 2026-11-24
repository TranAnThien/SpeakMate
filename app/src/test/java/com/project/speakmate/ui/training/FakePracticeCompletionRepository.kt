package com.project.speakmate.ui.training

import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionState
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakePracticeCompletionRepository : PracticeCompletionRepository {
    override fun setNetworkAvailable(isAvailable: Boolean) = Unit
    override fun resumePendingSaves() = Unit
    val state = MutableStateFlow<PracticeCompletionState>(PracticeCompletionState.Grading)
    val retriedIds = mutableListOf<String>()
    val retriedGradingIds = mutableListOf<String>()
    val unassessedIds = mutableListOf<String>()
    val releasedIds = mutableListOf<String>()

    override fun completePractice(practice: CompletedPractice): String? = null

    override fun observeCompletion(attemptId: String) = state

    override fun observePendingCompletions() = flowOf(emptyList<PendingPracticeCompletion>())

    override fun retryGrading(attemptId: String) {
        retriedGradingIds += attemptId
        if (state.value is PracticeCompletionState.AwaitingGrading) state.value = PracticeCompletionState.Grading
    }

    override fun saveUnassessed(attemptId: String) {
        unassessedIds += attemptId
    }

    override fun discardCompletion(attemptId: String) { releasedIds += attemptId }

    override fun retrySave(attemptId: String) {
        retriedIds += attemptId
        val current = state.value as? PracticeCompletionState.SaveFailed ?: return
        state.value = current.copy(isSaving = true)
    }

    override fun releaseCompletion(attemptId: String) {
        releasedIds += attemptId
    }
}
