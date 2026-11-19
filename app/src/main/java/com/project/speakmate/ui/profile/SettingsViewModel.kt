package com.project.speakmate.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.utils.extension.defaultFalse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val SIGN_OUT_CONFIRMATION = "sign_out_confirmation"
    }

    private val _uiState = MutableStateFlow(SettingsUiState(
        isSignOutConfirmationVisible = savedStateHandle.get<Boolean>(SIGN_OUT_CONFIRMATION).defaultFalse()
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun requestSignOut() {
        if (_uiState.value.isSigningOut) return
        savedStateHandle[SIGN_OUT_CONFIRMATION] = true
        _uiState.update { it.copy(isSignOutConfirmationVisible = true, isSignOutFailed = false) }
    }

    fun cancelSignOut() {
        if (_uiState.value.isSigningOut) return
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update { it.copy(isSignOutConfirmationVisible = false) }
    }

    fun confirmSignOut() {
        if (!_uiState.value.isSignOutConfirmationVisible || _uiState.value.isSigningOut) return
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update { it.copy(isSignOutConfirmationVisible = false, isSigningOut = true, isSignOutFailed = false) }
        viewModelScope.launch {
            try {
                val result = authRepository.signOut()
                _uiState.update { it.copy(isSigningOut = false, isSignOutFailed = result.isFailure) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSigningOut = false, isSignOutFailed = true) }
            }
        }
    }
}
