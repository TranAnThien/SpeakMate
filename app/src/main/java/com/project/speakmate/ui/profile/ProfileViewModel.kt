package com.project.speakmate.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _forceSignOutEvent = MutableSharedFlow<Unit>()
    val forceSignOutEvent: SharedFlow<Unit> = _forceSignOutEvent.asSharedFlow()

    private var hasAuthenticatedSession = false

    init {
        observeCurrentUser()
    }

    fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.getSignedInUserId().collect { uid ->
                if (uid != null) {
                    hasAuthenticatedSession = true
                    userRepository.getUser(uid).onSuccess { user ->
                        if (user != null) {
                            _userProfile.value = user
                        } else {
                            Log.e("ProfileViewModel", "Phát hiện tài khoản lỗi (Không có Firestore). Đang ép đăng xuất...")
                            forceSignOutAndNavigate()
                        }
                    }.onFailure { error ->
                        Log.e("ProfileViewModel", "Lỗi truy xuất Firestore. Đang ép đăng xuất...", error)
                        forceSignOutAndNavigate()
                    }
                } else {
                    val shouldNavigateToAuth = hasAuthenticatedSession
                    hasAuthenticatedSession = false
                    _userProfile.value = null
                    if (shouldNavigateToAuth) {
                        _forceSignOutEvent.emit(Unit)
                    }
                }
            }
        }
    }

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
        authRepository.signOut()
        _forceSignOutEvent.emit(Unit)
    }
}
