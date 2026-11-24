package com.project.speakmate.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.domain.repository.AppLanguageRepository
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.components.dialog.LegalDocument
import com.project.speakmate.ui.profile.state.SettingsDialog
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.utils.extension.defaultFalse
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
class SettingsViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val completionRepository: PracticeCompletionRepository,
    private val languageRepository: AppLanguageRepository
) : ViewModel() {
    companion object {
        private const val SIGN_OUT_CONFIRMATION = "sign_out_confirmation"
        private const val SIGN_OUT_USER_ID = "sign_out_user_id"
        private const val SIGN_OUT_PENDING_COUNT = "sign_out_pending_count"
        private const val LANGUAGE_SELECTION = "language_selection"
    }

    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                activeDialog =
                    when {
                        savedStateHandle.get<Boolean>(LANGUAGE_SELECTION).defaultFalse() ->
                            SettingsDialog.LanguageSelection
                        savedStateHandle.get<Boolean>(SIGN_OUT_CONFIRMATION).defaultFalse() ->
                            SettingsDialog.SignOutConfirmation
                        else -> null
                    },
                pendingCompletionCount = savedStateHandle.get<Int>(SIGN_OUT_PENDING_COUNT)
            )
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var pendingCompletionUserId: String? = null
    private var latestPendingCompletionCount: Int? = null
    private var languageJob: Job? = null
    private var pendingJob: Job? = null
    private var hasPendingObserverError = false

    init {
        retryLanguage()
        observePendingCompletions()
    }

    private fun observePendingCompletions() {
        pendingJob?.cancel()
        hasPendingObserverError = false
        pendingJob =
            viewModelScope.launch {
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        try {
                            updatePendingCount(uid = uid, count = null)
                            if (uid != null)
                                completionRepository.observePendingCompletions().collect { pending ->
                                    val count =
                                        if (authRepository.requireCurrentUserId() == uid)
                                            pending.distinctBy { it.attemptId }.size
                                        else null
                                    updatePendingCount(uid, count)
                                }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            showPendingObserverError()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showPendingObserverError()
                }
            }
    }

    private fun updatePendingCount(uid: String?, count: Int?) {
        pendingCompletionUserId = uid
        latestPendingCompletionCount = count
        val isConfirmationForCurrentUser = uid != null && uid == savedStateHandle.get<String>(SIGN_OUT_USER_ID)
        if (!isConfirmationForCurrentUser) savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update {
            val isConfirmationVisible = it.isSignOutConfirmationVisible && isConfirmationForCurrentUser
            it.copy(
                activeDialog =
                    if (isConfirmationVisible) SettingsDialog.SignOutConfirmation
                    else it.activeDialog.takeUnless { dialog -> dialog == SettingsDialog.SignOutConfirmation },
                pendingCompletionCount = if (isConfirmationVisible) it.pendingCompletionCount else count
            )
        }
    }

    private fun showPendingObserverError() {
        hasPendingObserverError = true
        pendingCompletionUserId = null
        latestPendingCompletionCount = null
        _uiState.update { if (it.isSignOutConfirmationVisible) it else it.copy(pendingCompletionCount = null) }
    }

    fun retry() {
        if (hasPendingObserverError || pendingJob?.isActive != true) observePendingCompletions()
    }

    fun showLanguageNotice() {
        if (_uiState.value.isSigningOut) return
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        savedStateHandle[LANGUAGE_SELECTION] = true
        _uiState.update { it.copy(activeDialog = SettingsDialog.LanguageSelection, isLanguageSaveFailed = false) }
    }

    fun hideLanguageSelection() {
        if (_uiState.value.isLanguageSaving) return
        savedStateHandle[LANGUAGE_SELECTION] = false
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun retryLanguage() {
        languageJob?.cancel()
        _uiState.update { it.copy(isLanguageLoading = true, hasLanguageLoadError = false) }
        languageJob =
            viewModelScope.launch {
                try {
                    languageRepository.observeLanguage().collect { language ->
                        _uiState.update {
                            it.copy(
                                selectedLanguage = language,
                                isLanguageLoading = false,
                                hasLanguageLoadError = false
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLanguageLoading = false, hasLanguageLoadError = true) }
                }
            }
    }

    fun changeLanguage(language: AppLanguage) {
        val state = _uiState.value
        if (state.isSigningOut ||
            state.isLanguageSaving ||
            state.isRecreateRequired ||
            state.isLanguageLoading ||
            state.hasLanguageLoadError ||
            !state.isLanguageSelectionVisible ||
            state.selectedLanguage == language)
            return
        _uiState.update { it.copy(isLanguageSaving = true, isLanguageSaveFailed = false) }
        viewModelScope.launch {
            try {
                languageRepository.saveLanguage(language).getOrThrow()
                _uiState.update {
                    it.copy(
                        selectedLanguage = language,
                        isLanguageSaving = false,
                        isRecreateRequired = true
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLanguageSaveFailed = true) }
            } finally {
                _uiState.update { it.copy(isLanguageSaving = false) }
            }
        }
    }

    fun consumeRecreateRequired(): Boolean {
        while (true) {
            val current = _uiState.value
            if (!current.isRecreateRequired) return false
            if (_uiState.compareAndSet(current, current.copy(isRecreateRequired = false))) return true
        }
    }

    fun showPrivacyNotice() = showNotice(LegalDocument.PRIVACY)

    fun showTermsNotice() = showNotice(LegalDocument.TERMS)

    fun dismissNotice() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun requestSignOut() {
        retry()
        if (_uiState.value.isSigningOut) return
        val uid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                return
            }
        savedStateHandle[SIGN_OUT_USER_ID] = uid
        val pendingCount = if (pendingCompletionUserId == uid) latestPendingCompletionCount else null
        savedStateHandle[SIGN_OUT_PENDING_COUNT] = pendingCount
        savedStateHandle[LANGUAGE_SELECTION] = false
        savedStateHandle[SIGN_OUT_CONFIRMATION] = true
        _uiState.update {
            it.copy(
                activeDialog = SettingsDialog.SignOutConfirmation,
                isSignOutFailed = false,
                pendingCompletionCount = pendingCount
            )
        }
    }

    fun cancelSignOut() {
        if (_uiState.value.isSigningOut) return
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun confirmSignOut() {
        if (!_uiState.value.isSignOutConfirmationVisible || _uiState.value.isSigningOut) return
        val uid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                null
            }
        if (uid == null || uid != savedStateHandle.get<String>(SIGN_OUT_USER_ID)) {
            cancelSignOut()
            return
        }
        val confirmedCount = _uiState.value.pendingCompletionCount
        val pendingCount = if (pendingCompletionUserId == uid) latestPendingCompletionCount else null
        if (confirmedCount != null && (pendingCount == null || pendingCount > confirmedCount)) {
            savedStateHandle[SIGN_OUT_PENDING_COUNT] = pendingCount
            _uiState.update { it.copy(pendingCompletionCount = pendingCount) }
            return
        }
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update { it.copy(activeDialog = null, isSigningOut = true, isSignOutFailed = false) }
        viewModelScope.launch {
            try {
                val signedInUid =
                    try {
                        authRepository.requireCurrentUserId()
                    } catch (_: IllegalStateException) {
                        null
                    }
                if (signedInUid != uid) {
                    _uiState.value = SettingsUiState()
                    return@launch
                }
                val result = authRepository.signOut()
                val currentUid =
                    try {
                        authRepository.requireCurrentUserId()
                    } catch (_: IllegalStateException) {
                        null
                    }
                if (currentUid == null || currentUid == uid) {
                    _uiState.update { it.copy(isSigningOut = false, isSignOutFailed = result.isFailure) }
                } else _uiState.value = SettingsUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val currentUid =
                    try {
                        authRepository.requireCurrentUserId()
                    } catch (_: IllegalStateException) {
                        null
                    }
                if (currentUid == uid) _uiState.update { it.copy(isSigningOut = false, isSignOutFailed = true) }
                else _uiState.value = SettingsUiState()
            }
        }
    }

    private fun showNotice(notice: LegalDocument) {
        if (_uiState.value.isSigningOut) return
        savedStateHandle[LANGUAGE_SELECTION] = false
        savedStateHandle[SIGN_OUT_CONFIRMATION] = false
        _uiState.update { it.copy(activeDialog = SettingsDialog.Notice(notice)) }
    }
}
