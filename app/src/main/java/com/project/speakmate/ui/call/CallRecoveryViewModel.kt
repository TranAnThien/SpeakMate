package com.project.speakmate.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.CallRecoveryRepository
import com.project.speakmate.ui.call.state.CallRecoveryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CallRecoveryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recoveryRepository: CallRecoveryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CallRecoveryUiState())
    val uiState: StateFlow<CallRecoveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _uiState.update { CallRecoveryUiState(userId = uid) }
                    val interrupted = try {
                        uid != null && recoveryRepository.consumeInterruptedCall(uid)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        false
                    }
                    _uiState.update { CallRecoveryUiState(uid, false, interrupted, interrupted) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { CallRecoveryUiState(isLoading = false) }
            }
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(isNoticeVisible = false) }
    }
}
