package com.project.speakmate.ui.history

import com.project.speakmate.utils.AppConstant
import androidx.navigation.toRoute
import com.project.speakmate.app.AppRoute
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.utils.extension.default
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.project.speakmate.domain.model.PracticeAttempt
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.durationInSeconds
import com.project.speakmate.domain.repository.HistoryRepository
import com.project.speakmate.domain.repository.PracticeRepository
import com.project.speakmate.ui.history.state.SessionActionUiState
import com.project.speakmate.ui.history.state.SessionCriterionUiState
import com.project.speakmate.ui.history.state.SessionDetailPresentation
import com.project.speakmate.ui.history.state.SessionDetailUiState
import com.project.speakmate.ui.history.state.SessionMessageUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val attemptId: String = try { savedStateHandle.toRoute<AppRoute.SessionDetail>().attemptId } catch (_: IllegalArgumentException) { "" }

    private val _uiState = MutableStateFlow(SessionDetailUiState(presentation = SessionDetailPresentation.LOADING))
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat(AppConstant.DISPLAY_DATE_TIME_PATTERN, Locale.getDefault())

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
                _uiState.update { toDetailUiState(attempt, scenarioTitle) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { SessionDetailUiState(presentation = SessionDetailPresentation.LOAD_ERROR) }
            }
        }
    }

    private fun toDetailUiState(attempt: PracticeAttempt, scenarioTitle: String): SessionDetailUiState {
        val durationFormatted = String.format(Locale.ROOT, AppConstant.DURATION_FORMAT, attempt.durationInSeconds / AppConstant.SECONDS_PER_MINUTE, attempt.durationInSeconds % AppConstant.SECONDS_PER_MINUTE)
        val dateFormatted = dateFormatter.format(attempt.startedAt)

        val values = attempt.criteriaResult?.let { listOf(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet) }
        val criteriaList = if (values == null) emptyList() else AssessmentCriterionType.entries.mapIndexed { index, type ->
            SessionCriterionUiState(id = type.name, isPassed = values[index], criterion = type)
        }

        val actionList = attempt.requiredActionResults.map { (actionId, passed) ->
            SessionActionUiState(actionId, actionId, passed)
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
