package com.project.speakmate.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.InvalidUserProfileException
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

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
            authRepository.observeSignedInUserId().collect { uid ->
                if (uid != null) {
                    hasAuthenticatedSession = true
                    userRepository.getUser(uid).onSuccess { user ->
                        if (user != null) {
                            _userProfile.value = user
                            _isProfileLoadFailed.value = false
                        } else {
                            Log.e(TAG, "Signed-in user has no profile document")
                            forceSignOutAndNavigate()
                        }
                    }.onFailure { error ->
                        if (error is InvalidUserProfileException) {
                            _isProfileLoadFailed.value = true
                            _userProfile.value = null
                        } else {
                            Log.e(TAG, "Could not load user profile")
                            forceSignOutAndNavigate()
                        }
                    }
                } else {
                    val shouldNavigateToAuth = hasAuthenticatedSession
                    hasAuthenticatedSession = false
                    _userProfile.value = null
                    _isProfileLoadFailed.value = false
                    if (shouldNavigateToAuth) {
                        _forceSignOutEvent.emit(Unit)
                    }
                }
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

    private suspend fun forceSignOutAndNavigate() {
        hasAuthenticatedSession = false
        _userProfile.value = null
        _isProfileLoadFailed.value = false
        authRepository.signOut()
        _forceSignOutEvent.emit(Unit)
    }
}
