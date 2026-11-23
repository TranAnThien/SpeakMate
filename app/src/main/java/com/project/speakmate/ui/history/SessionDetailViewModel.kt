package com.project.speakmate.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.history.state.SessionCriterionUiState
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.ui.history.state.SessionMessageUiState
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.toDateTimeLabel
import com.project.speakmate.utils.extension.toDurationLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository,
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val attemptId: String = try { savedStateHandle.toRoute<AppRoute.SessionDetail>().attemptId } catch (_: IllegalArgumentException) { "" }

    private val _uiState = MutableStateFlow(SessionDetailUiState(presentation = SessionDetailPresentation.LOADING))
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadSessionDetail()
    }

    fun loadSessionDetail() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { SessionDetailUiState(presentation = SessionDetailPresentation.LOADING) }
            try {
                check(attemptId.isNotBlank())
                val attempt = historyRepository.getSessionDetail(attemptId).getOrThrow()
                val scenarioTitle = practiceRepository.getScenario(attempt.scenarioId).getOrNull()?.name
                    ?.takeIf(String::isNotBlank).default(attempt.scenarioId)
                val missingIds = attempt.requiredActionResults.keys.filter { it !in attempt.requiredActionDescriptions }.toSet()
                val resolvedDescriptions = if (missingIds.isNotEmpty()) {
                    resolveActionDescriptions(missingIds)
                } else emptyMap()
                val allDescriptions = attempt.requiredActionDescriptions + resolvedDescriptions
                _uiState.update { toDetailUiState(attempt, scenarioTitle, allDescriptions) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR) }
            }
        }
    }

    private suspend fun resolveActionDescriptions(actionIds: Set<String>): Map<String, String> {
        if (actionIds.isEmpty()) return emptyMap()
        val descriptions = mutableMapOf<String, String>()
        val primaryLevel = actionIds.firstNotNullOfOrNull { id ->
            SpeakingLevel.entries.firstOrNull { level -> level != SpeakingLevel.NOT_ASSESSED && id.startsWith(level.name, ignoreCase = true) }
        }
        val levelsToTry = if (primaryLevel != null) {
            listOf(primaryLevel) + SpeakingLevel.entries.filter { it != SpeakingLevel.NOT_ASSESSED && it != primaryLevel }
        } else {
            SpeakingLevel.entries.filter { it != SpeakingLevel.NOT_ASSESSED }
        }
        for (level in levelsToTry) {
            val path = trainingRepository.getTrainingPath(level).getOrNull() ?: continue
            path.goals.forEach { goal ->
                goal.units.forEach { unit ->
                    unit.requiredActions.forEach { action ->
                        descriptions[action.requiredActionId] = action.description
                    }
                }
                goal.goalAssessment.requiredActions.forEach { action ->
                    descriptions[action.requiredActionId] = action.description
                }
            }
            if (actionIds.all { it in descriptions }) break
        }
        return descriptions
    }

    private fun toDetailUiState(
        attempt: PracticeAttempt,
        scenarioTitle: String,
        actionDescriptions: Map<String, String> = emptyMap()
    ): SessionDetailUiState {
        val durationFormatted = attempt.durationInSeconds.toDurationLabel()
        val dateFormatted = attempt.startedAt.toDateTimeLabel()

        val values = attempt.criteriaResult?.let { listOf(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet) }
        val criteriaList = if (values == null) emptyList() else AssessmentCriterionType.entries.mapIndexed { index, type ->
            SessionCriterionUiState(id = type.name, isPassed = values[index], criterion = type)
        }

        val actionList = attempt.requiredActionResults.map { (actionId, passed) ->
            val text = actionDescriptions[actionId]?.takeIf(String::isNotBlank) ?: actionId
            SessionActionUiState(actionId, text, passed)
        }

        val messageList = attempt.transcripts.sortedBy { it.orderIndex }.map { msg ->
            SessionMessageUiState(
                id = msg.messageId,
                sender = msg.senderType,
                isInterrupted = msg.senderType == SenderType.AI && msg.isInterrupted,
                text = msg.content,
                isUser = msg.senderType == SenderType.USER
            )
        }

        return SessionDetailUiState(
            presentation = SessionDetailPresentation.COMPLETED,
            title = scenarioTitle,
            metadata = "$dateFormatted • $durationFormatted",
            result = HistoryResultMapper.toPresentation(attempt.result, attempt.practiceType),
            feedbackSummary = attempt.feedbackSummary,
            requiredActions = actionList,
            criteria = criteriaList,
            messages = messageList
        )
    }
}
