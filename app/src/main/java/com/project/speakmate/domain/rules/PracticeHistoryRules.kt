package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.PendingPracticeCompletion
import com.project.speakmate.domain.model.PracticeCompletionStatus
import com.project.speakmate.domain.model.PracticeHistoryPage
import com.project.speakmate.domain.model.PracticeHistoryCursor
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.PracticeType
import com.project.speakmate.domain.model.TrainingPath

object PracticeHistoryRules {
    fun mergePage(
        attempts: List<PracticeAttempt>,
        page: PracticeHistoryPage,
        cursor: PracticeHistoryCursor?,
        isRefresh: Boolean
    ): PracticeHistoryPage {
        check(!page.hasMore || page.nextCursor != null && page.nextCursor != cursor)
        return page.copy(
            attempts =
                if (isRefresh) page.attempts.distinctBy { it.attemptId } else mergeAttempts(attempts, page.attempts)
        )
    }

    fun getPendingBridge(
        removed: List<PendingPracticeCompletion>,
        attempts: List<PracticeAttempt>
    ): List<PendingPracticeCompletion> =
        removed
            .filter { previous -> attempts.none { it.attemptId == previous.attemptId } }
            .map { it.copy(status = PracticeCompletionStatus.SAVING) }

    fun mergeAttempts(first: List<PracticeAttempt>, second: List<PracticeAttempt>): List<PracticeAttempt> =
        (first + second).distinctBy { it.attemptId }

    fun getActionDescriptions(scenarioId: String, practiceType: PracticeType, paths: List<TrainingPath>): Map<String, String> {
        val actions = when (practiceType) {
            PracticeType.TRAINING_UNIT -> paths.flatMap { path -> path.goals.flatMap { it.units } }
                .filter { it.scenarioId == scenarioId }.flatMap { it.requiredActions }
            PracticeType.GOAL_ASSESSMENT -> paths.flatMap { it.goals }.map { it.goalAssessment }
                .filter { it.scenarioId == scenarioId }.flatMap { it.requiredActions }
            PracticeType.FREE_PRACTICE -> emptyList()
        }
        return actions.groupBy { it.requiredActionId }.mapNotNull { (id, matches) ->
            matches.map { it.description }.filter(String::isNotBlank).distinct().singleOrNull()?.let { id to it }
        }.toMap()
    }
}
