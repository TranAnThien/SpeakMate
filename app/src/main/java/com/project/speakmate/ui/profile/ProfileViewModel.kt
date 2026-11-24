package com.project.speakmate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.SavedVocabularyRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.rules.SavedVocabularyRules
import com.project.speakmate.ui.components.assessment.AssessmentCriteriaUiMapper
import com.project.speakmate.ui.profile.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val savedVocabularyRepository: SavedVocabularyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var observedUserId: String? = null
    private var profileObservationJob: Job? = null

    init {
        observeCurrentUser()
    }

    fun observeCurrentUser() {
        profileObservationJob?.cancel()
        profileObservationJob =
            viewModelScope.launch {
                try {
                    authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                        if (uid != observedUserId) {
                            observedUserId = uid
                            _uiState.value = ProfileUiState()
                        }
                        _uiState.update { it.copy(isLoading = uid != null, isProfileLoadFailed = false) }
                        if (uid != null) {
                            try {
                                coroutineScope {
                                    launch {
                                        savedVocabularyRepository
                                            .observeSavedVocabularies()
                                            .catch { failure -> if (failure is CancellationException) throw failure }
                                            .collect { vocabulary ->
                                                _uiState.update {
                                                    it.copy(
                                                        savedVocabularyCount =
                                                            vocabulary
                                                                .distinctBy(SavedVocabularyRules::getIdentity)
                                                                .size
                                                    )
                                                }
                                            }
                                    }
                                    userRepository.observeUser(uid).collect { user ->
                                        if (authRepository.requireCurrentUserId() == uid && observedUserId == uid) {
                                            if (user != null && user.userId == uid) _uiState.value = toUiState(user)
                                            else
                                                _uiState.update {
                                                    it.copy(isLoading = false, isProfileLoadFailed = true)
                                                }
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
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    showProfileLoadFailure()
                }
            }
    }

    fun retryProfile() = observeCurrentUser()

    private fun showProfileLoadFailure() {
        val uid =
            try {
                authRepository.requireCurrentUserId()
            } catch (_: IllegalStateException) {
                null
            }
        if (uid != null && uid == observedUserId) {
            _uiState.update { it.copy(isLoading = false, isProfileLoadFailed = true) }
        } else {
            observedUserId = null
            _uiState.value = ProfileUiState(isLoading = false, isProfileLoadFailed = true)
        }
    }

    private fun toUiState(user: User): ProfileUiState {
        val criteria = user.latestAssessmentCriteria
        return ProfileUiState(
            displayName = user.displayName,
            email = user.email,
            avatarUrl = user.photoUrl,
            level = user.level,
            isLoading = false,
            hasLoaded = true,
            lastAssessedAtMillis = user.lastAssessedAt?.time,
            savedVocabularyCount = _uiState.value.savedVocabularyCount,
            skills =
                AssessmentCriteriaUiMapper.toCriteria(criteria.takeUnless { user.level == SpeakingLevel.NOT_ASSESSED })
        )
    }
}
