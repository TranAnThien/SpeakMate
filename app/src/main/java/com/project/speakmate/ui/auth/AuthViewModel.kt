package com.project.speakmate.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.auth.state.GoogleSignInUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleSignInUiState())
    val uiState: StateFlow<GoogleSignInUiState> = _uiState.asStateFlow()

    private val _signInSuccess = MutableSharedFlow<Unit>()
    val signInSuccess: SharedFlow<Unit> = _signInSuccess.asSharedFlow()

    fun handleGoogleSignIn(idToken: String, displayName: String?, email: String?, photoUrl: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = GoogleSignInStatus.LOADING) }

            val authResult = authRepository.signInWithGoogle(idToken)
            authResult.onSuccess { userId ->
                checkAndInitializeUser(userId, displayName, email, photoUrl)
            }.onFailure { error ->
                Log.e("AuthViewModel", "FirebaseAuth failed", error)
                _uiState.update { it.copy(status = GoogleSignInStatus.ERROR) }
            }
        }
    }

    fun onGoogleSignInCancelled() {
        _uiState.update { it.copy(status = GoogleSignInStatus.NORMAL) }
    }

    fun onGoogleSignInError(error: Throwable) {
        Log.e("AuthViewModel", "Google Sign-In failed", error)
        _uiState.update { it.copy(status = GoogleSignInStatus.ERROR) }
    }

    private suspend fun checkAndInitializeUser(userId: String, displayName: String?, email: String?, photoUrl: String?) {
        val defaultUser = User(
            userId = userId,
            displayName = displayName ?: "",
            email = email ?: "",
            photoUrl = photoUrl ?: "",
            level = SpeakingLevel.NOT_ASSESSED,
            savedVocabularies = emptyList()
        )

        val result = userRepository.getOrCreateUser(defaultUser)
        result.onSuccess {
            _uiState.update { it.copy(status = GoogleSignInStatus.NORMAL) }
            _signInSuccess.emit(Unit)
        }.onFailure { error ->
            Log.e("AuthViewModel", "Firestore getOrCreateUser failed", error)
            authRepository.signOut()
            _uiState.update { it.copy(status = GoogleSignInStatus.ERROR) }
        }
    }

    fun resetState() {
        _uiState.update { it.copy(status = GoogleSignInStatus.NORMAL) }
    }
}
