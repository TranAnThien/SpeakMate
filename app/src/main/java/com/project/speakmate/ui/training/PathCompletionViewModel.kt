package com.project.speakmate.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.ui.training.state.PathCompletionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PathCompletionViewModel
@Inject
constructor(private val authRepository: AuthRepository, private val userRepository: UserRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PathCompletionUiState())
    val uiState: StateFlow<PathCompletionUiState> = _uiState.asStateFlow()
    private var observationJob: Job? = null

    init {
        observeCompletion()
    }

    fun retry() {
        observeCompletion()
    }

    private fun observeCompletion() {
        observationJob?.cancel()
        observationJob =
            viewModelScope.launch {
                _uiState.update { PathCompletionUiState() }
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        _uiState.update { PathCompletionUiState() }
                        if (uid == null) {
                            _uiState.update { PathCompletionUiState(isLoading = false, hasLoadError = true) }
                            return@collectLatest
                        }
                        userRepository.observeUser(uid).collect { user ->
                            check(authRepository.requireCurrentUserId() == uid)
                            val isCompleted =
                                user?.userId == uid &&
                                    user.level != SpeakingLevel.NOT_ASSESSED &&
                                    user.trainingProgress?.stage == TrainingStage.PATH_COMPLETED
                            _uiState.update {
                                PathCompletionUiState(
                                    isLoading = false,
                                    isCompleted = isCompleted,
                                    level = if (isCompleted) checkNotNull(user).level else SpeakingLevel.NOT_ASSESSED,
                                    nextLevel =
                                        if (isCompleted) checkNotNull(user).level.getNextLevel()
                                        else null
                                )
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.update { PathCompletionUiState(isLoading = false, hasLoadError = true) }
                }
            }
    }
}
