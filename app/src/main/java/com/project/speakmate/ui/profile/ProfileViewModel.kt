package com.project.speakmate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.ui.profile.state.ProfileUiState
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.utils.extension.default
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
                    _uiState.update { ProfileUiState() }
                    if (uid == null) {
                        val shouldNavigateToAuth = hasAuthenticatedSession
                        hasAuthenticatedSession = false
                        if (shouldNavigateToAuth) _forceSignOutEvent.emit(Unit)
                    } else {
                        hasAuthenticatedSession = true
                        try {
                            userRepository.observeUser(uid).collect { user ->
                                _uiState.update { user?.let(::toUiState).default(ProfileUiState(isProfileLoadFailed = true)) }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            _uiState.update { ProfileUiState(isProfileLoadFailed = true) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { ProfileUiState(isProfileLoadFailed = true) }
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

    private fun toUiState(user: User): ProfileUiState {
        val criteria = user.latestAssessmentCriteria
        val values = criteria?.let { listOf(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet) }
        return ProfileUiState(
            displayName = user.displayName,
            email = user.email,
            avatarUrl = user.photoUrl,
            level = user.level,
            skills = if (values == null) emptyList() else AssessmentCriterionType.entries.mapIndexed { index, type ->
                ProfileSkillUiState(id = type.name, isPassed = values[index], criterion = type)
            }
        )
    }

}
