package com.project.speakmate.ui.profile

import androidx.lifecycle.ViewModel
import com.project.speakmate.ui.profile.state.ProfileEffect
import androidx.lifecycle.viewModelScope
import com.project.speakmate.ui.components.AssessmentCriterionType
import com.project.speakmate.ui.profile.state.ProfileUiState
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import kotlinx.coroutines.flow.update
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.domain.model.SpeakingLevel
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

    private val _uiEffect = MutableSharedFlow<ProfileEffect>(replay = 0)
    val uiEffect: SharedFlow<ProfileEffect> = _uiEffect.asSharedFlow()

    private var observedUserId: String? = null
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
                    if (uid != observedUserId) {
                        observedUserId = uid
                        _uiState.value = ProfileUiState()
                    }
                    _uiState.update { it.copy(isLoading = uid != null, isProfileLoadFailed = false) }
                    if (uid == null) {
                        val shouldNavigateToAuth = hasAuthenticatedSession
                        hasAuthenticatedSession = false
                        if (shouldNavigateToAuth) _uiEffect.emit(ProfileEffect.SignedOut)
                    } else {
                        hasAuthenticatedSession = true
                        try {
                            userRepository.observeUser(uid).collect { user ->
                                if (authRepository.requireCurrentUserId() == uid && observedUserId == uid) {
                                    if (user != null && user.userId == uid) _uiState.value = toUiState(user)
                                    else _uiState.update { it.copy(isLoading = false, isProfileLoadFailed = true) }
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            showProfileLoadFailure()
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showProfileLoadFailure()
            }
        }
    }

    fun retryProfile() = observeCurrentUser()

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut().getOrThrow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                showProfileLoadFailure()
            }
        }
    }

    private fun showProfileLoadFailure() {
        val uid = try { authRepository.requireCurrentUserId() } catch (_: IllegalStateException) { null }
        if (uid != null && uid == observedUserId) {
            _uiState.update { it.copy(isLoading = false, isProfileLoadFailed = true) }
        } else {
            observedUserId = null
            _uiState.value = ProfileUiState(isLoading = false, isProfileLoadFailed = true)
        }
    }

    private fun toUiState(user: User): ProfileUiState {
        val criteria = user.latestAssessmentCriteria
        val values = if (user.level == SpeakingLevel.NOT_ASSESSED) null else criteria?.let {
            listOf(it.rangeMet, it.accuracyMet, it.fluencyMet, it.interactionMet, it.coherenceMet)
        }
        return ProfileUiState(
            displayName = user.displayName,
            email = user.email,
            avatarUrl = user.photoUrl,
            level = user.level,
            isLoading = false,
            hasLoaded = true,
            lastAssessedAtMillis = user.lastAssessedAt?.time,
            savedVocabularyCount = user.savedVocabularies.distinctBy(SavedVocabularyRules::getIdentity).size,
            skills = if (values == null) emptyList() else AssessmentCriterionType.entries.mapIndexed { index, type ->
                ProfileSkillUiState(id = type.name, isPassed = values[index], criterion = type)
            }
        )
    }

}
