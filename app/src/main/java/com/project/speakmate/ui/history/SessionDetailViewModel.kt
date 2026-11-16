package com.project.speakmate.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val attemptId: String = checkNotNull(savedStateHandle["attemptId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState(presentation = SessionDetailPresentation.LOADING))
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        loadSessionDetail()
    }

    fun loadSessionDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(presentation = SessionDetailPresentation.LOADING) }
            historyRepository.getSessionDetail(attemptId)
                .onSuccess { attempt ->
                    val scenarioTitle = practiceRepository.getScenario(attempt.scenarioId).getOrNull()?.name?.takeIf { it.isNotBlank() }
                        ?: attempt.scenarioId.ifBlank { "Chi tiết phiên luyện tập" }
                    _uiState.value = toDetailUiState(attempt, scenarioTitle)
                }
                .onFailure {
                    _uiState.update { it.copy(presentation = SessionDetailPresentation.LOAD_ERROR) }
                }
        }
    }

    private fun toDetailUiState(attempt: PracticeAttempt, scenarioTitle: String): SessionDetailUiState {
        val durationFormatted = String.format(Locale.ROOT, "%02d:%02d", attempt.durationInSeconds / 60, attempt.durationInSeconds % 60)
        val dateFormatted = dateFormatter.format(attempt.endedAt)

        val criteriaList = attempt.criteriaResult?.let { criteria ->
            listOf(
                SessionCriterionUiState("range", "Phạm vi từ vựng", "Sử dụng từ vựng phù hợp ngữ cảnh", criteria.rangeMet),
                SessionCriterionUiState("accuracy", "Độ chính xác", "Cấu trúc ngữ pháp chuẩn xác", criteria.accuracyMet),
                SessionCriterionUiState("fluency", "Độ trôi chảy", "Tốc độ nói và phản xạ liên tục", criteria.fluencyMet),
                SessionCriterionUiState("interaction", "Tương tác", "Phản hồi câu hỏi tự nhiên", criteria.interactionMet),
                SessionCriterionUiState("coherence", "Mạch lạc", "Liên kết ý logic và rõ nghĩa", criteria.coherenceMet)
            )
        } ?: emptyList()

        val actionList = attempt.requiredActionResults.map { (actionId, passed) ->
            SessionActionUiState(actionId, actionId, passed)
        }

        val messageList = attempt.transcripts.sortedBy { it.orderIndex }.map { msg ->
            SessionMessageUiState(
                id = msg.messageId,
                metadata = if (msg.senderType == SenderType.USER) "Bạn" else "AI",
                text = msg.content,
                isUser = msg.senderType == SenderType.USER
            )
        }

        return SessionDetailUiState(
            presentation = SessionDetailPresentation.COMPLETED,
            title = scenarioTitle,
            metadata = "$dateFormatted • $durationFormatted",
            criteriaHeading = attempt.feedbackSummary ?: "Kết quả đánh giá",
            requiredActions = actionList,
            criteria = criteriaList,
            messages = messageList
        )
    }
}
