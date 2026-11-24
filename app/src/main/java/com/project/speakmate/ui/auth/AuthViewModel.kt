package com.project.speakmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.auth.state.GoogleSignInUiState
import com.project.speakmate.ui.components.dialog.LegalDocument
import com.project.speakmate.utils.extension.defaultEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel
@Inject
constructor(private val authRepository: AuthRepository, private val userRepository: UserRepository) : ViewModel() {
    val signedInUserId: Flow<String?> = authRepository.observeSignedInUserId()
    private val _uiState = MutableStateFlow(GoogleSignInUiState())
    val uiState: StateFlow<GoogleSignInUiState> = _uiState.asStateFlow()
    private var signInJob: Job? = null
    private var profileJob: Job? = null
    private var observerJob: Job? = null
    private var signOutJob: Job? = null
    private var isCredentialRequestActive = false

    init {
        observeSession()
    }

    private fun observeSession() {
        observerJob?.cancel()
        observerJob =
            viewModelScope.launch {
                try {
                    signedInUserId.distinctUntilChanged().collectLatest { uid ->
                        try {
                            profileJob?.cancel()
                            if (uid == null) {
                                signInJob?.cancel()
                                isCredentialRequestActive = false
                                _uiState.update {
                                    GoogleSignInUiState(
                                        isCheckingProfile = false,
                                        status =
                                            if (signOutJob?.isActive == true) GoogleSignInStatus.SIGNING_OUT
                                            else GoogleSignInStatus.NORMAL
                                    )
                                }
                            } else {
                                _uiState.update {
                                    GoogleSignInUiState(userId = uid, status = GoogleSignInStatus.LOADING)
                                }
                                profileJob =
                                    viewModelScope.launch {
                                        signInJob?.join()
                                        prepareProfile(uid)
                                    }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            showProfileError()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showProfileError()
                }
            }
    }

    private fun showProfileError() {
        _uiState.update {
            it.copy(
                status = if (it.userId == null) GoogleSignInStatus.ERROR else GoogleSignInStatus.PROFILE_ERROR,
                isCheckingProfile = false,
                isProfileReady = false
            )
        }
    }

    fun startSignIn(): Boolean {
        if (!authRepository.isNetworkAvailable()) {
            showOffline()
            return false
        }
        if (observerJob?.isActive != true) observeSession()
        val state = _uiState.value
        if (isCredentialRequestActive ||
            signInJob?.isActive == true ||
            signOutJob?.isActive == true ||
            state.isCheckingProfile ||
            state.isProfileReady ||
            state.userId != null)
            return false
        isCredentialRequestActive = true
        _uiState.update { it.copy(status = GoogleSignInStatus.LOADING, legalDocument = null) }
        return true
    }

    fun handleGoogleSignIn(idToken: String, displayName: String?, email: String?, photoUrl: String?) {
        if (!isCredentialRequestActive || signInJob?.isActive == true) return
        isCredentialRequestActive = false
        signInJob =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                var operationUserId: String? = null
                try {
                    val uid = authRepository.signInWithGoogle(idToken).getOrThrow()
                    operationUserId = uid
                    if (!isCurrentUser(uid)) return@launch
                    userRepository
                        .getOrCreateUser(
                            User(
                                userId = uid,
                                displayName = displayName.defaultEmpty(),
                                email = email.defaultEmpty(),
                                photoUrl = photoUrl.defaultEmpty(),
                                level = SpeakingLevel.NOT_ASSESSED,
                                savedVocabularies = emptyList()
                            )
                        )
                        .getOrThrow()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val uid = operationUserId
                    if (uid != null && !isCurrentUser(uid)) return@launch
                    if (uid == null && _uiState.value.userId != null) return@launch
                    _uiState.update {
                        it.copy(
                            status =
                                if (it.userId == null) GoogleSignInStatus.ERROR else GoogleSignInStatus.PROFILE_ERROR,
                            isCheckingProfile = false,
                            isProfileReady = false
                        )
                    }
                }
            }
        signInJob?.start()
    }

    fun cancelSignIn() {
        if (!isCredentialRequestActive) return
        isCredentialRequestActive = false
        _uiState.update { it.copy(status = GoogleSignInStatus.CANCELLED) }
    }

    fun showSignInError(error: Throwable) {
        if (error is CancellationException) throw error
        isCredentialRequestActive = false
        _uiState.update {
            it.copy(
                status =
                    if (authRepository.isNetworkAvailable()) GoogleSignInStatus.ERROR else GoogleSignInStatus.OFFLINE
            )
        }
    }

    fun showOffline() {
        if (_uiState.value.userId != null ||
            _uiState.value.status == GoogleSignInStatus.LOADING ||
            signOutJob?.isActive == true)
            return
        _uiState.update { it.copy(status = GoogleSignInStatus.OFFLINE) }
    }

    fun retryProfile() {
        if (observerJob?.isActive != true) {
            observeSession()
            return
        }
        val uid = _uiState.value.userId ?: return
        if (profileJob?.isActive == true || signInJob?.isActive == true || !isCurrentUser(uid)) return
        _uiState.update {
            it.copy(status = GoogleSignInStatus.LOADING, isCheckingProfile = true, isProfileReady = false)
        }
        profileJob = viewModelScope.launch { prepareProfile(uid) }
    }

    fun signOut() {
        val uid = _uiState.value.userId ?: return
        if (_uiState.value.isCheckingProfile || signOutJob?.isActive == true) return
        _uiState.update { it.copy(isCheckingProfile = false, status = GoogleSignInStatus.SIGNING_OUT) }
        profileJob?.cancel()
        signOutJob =
            viewModelScope.launch {
                try {
                    if (isCurrentUser(uid)) authRepository.signOut().getOrThrow()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrentUser(uid))
                        _uiState.update {
                            it.copy(status = GoogleSignInStatus.PROFILE_ERROR, isCheckingProfile = false)
                        }
                } finally {
                    if (_uiState.value.userId == null)
                        _uiState.update { it.copy(status = GoogleSignInStatus.NORMAL, isCheckingProfile = false) }
                }
            }
    }

    fun isSessionReady(userId: String): Boolean =
        _uiState.value.userId == userId && _uiState.value.isProfileReady && isCurrentUser(userId)

    fun showTerms() {
        _uiState.update { it.copy(legalDocument = LegalDocument.TERMS) }
    }

    fun showPrivacyPolicy() {
        _uiState.update { it.copy(legalDocument = LegalDocument.PRIVACY) }
    }

    fun dismissLegalInformation() {
        _uiState.update { it.copy(legalDocument = null) }
    }

    private suspend fun prepareProfile(uid: String) {
        try {
            val user = checkNotNull(userRepository.getUser(uid).getOrThrow())
            check(user.userId == uid)
            if (!isCurrentUser(uid) || _uiState.value.userId != uid) return
            _uiState.update {
                it.copy(status = GoogleSignInStatus.NORMAL, isCheckingProfile = false, isProfileReady = true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (isCurrentUser(uid) && _uiState.value.userId == uid) {
                _uiState.update {
                    it.copy(
                        status = GoogleSignInStatus.PROFILE_ERROR,
                        isCheckingProfile = false,
                        isProfileReady = false
                    )
                }
            }
        }
    }

    private fun isCurrentUser(uid: String): Boolean =
        try {
            authRepository.requireCurrentUserId() == uid
        } catch (_: IllegalStateException) {
            false
        }
}
