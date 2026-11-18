package com.project.speakmate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()
    private val _isProfileLoadFailed = MutableStateFlow(false)
    val isProfileLoadFailed: StateFlow<Boolean> = _isProfileLoadFailed.asStateFlow()

    private val _forceSignOutEvent = MutableSharedFlow<Unit>()
    val forceSignOutEvent: SharedFlow<Unit> = _forceSignOutEvent.asSharedFlow()

    private var hasAuthenticatedSession = false
    private var profileObservationJob: Job? = null

    init {
        observeCurrentUser()
    }

    fun observeCurrentUser() {
        profileObservationJob?.cancel()
        profileObservationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _userProfile.value = null
                    _isProfileLoadFailed.value = false
                    if (uid == null) {
                        val shouldNavigateToAuth = hasAuthenticatedSession
                        hasAuthenticatedSession = false
                        if (shouldNavigateToAuth) _forceSignOutEvent.emit(Unit)
                    } else {
                        hasAuthenticatedSession = true
                        try {
                            userRepository.observeUser(uid).collect { user ->
                                _userProfile.value = user
                                _isProfileLoadFailed.value = user == null
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            _userProfile.value = null
                            _isProfileLoadFailed.value = true
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _userProfile.value = null
                _isProfileLoadFailed.value = true
            }
        }
    }

    fun retryProfile() = observeCurrentUser()

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            hasAuthenticatedSession = false
            authRepository.signOut()
            onComplete()
        }
    }

}
