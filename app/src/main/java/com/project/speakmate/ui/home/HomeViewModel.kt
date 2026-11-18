package com.project.speakmate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingStage
import com.project.speakmate.domain.model.User
import com.project.speakmate.domain.repository.AuthRepository
import com.project.speakmate.domain.repository.UserRepository
import com.project.speakmate.domain.repository.TrainingRepository
import com.project.speakmate.ui.home.state.HomePresentation
import com.project.speakmate.ui.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var userObservationJob: Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        userObservationJob?.cancel()
        userObservationJob = viewModelScope.launch {
            try {
                authRepository.observeSignedInUserId().distinctUntilChanged().collectLatest { uid ->
                    _uiState.update { HomeUiState() }
                    if (uid != null) {
                        try {
                            userRepository.observeUser(uid).collectLatest { user -> updateUserData(user) }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            _uiState.update { it.copy(presentation = HomePresentation.PROFILE_LOAD_ERROR) }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(presentation = HomePresentation.PROFILE_LOAD_ERROR) }
            }
        }
    }

    private suspend fun updateUserData(user: User?) {
        if (user == null) {
            _uiState.update { it.copy(presentation = HomePresentation.PROFILE_LOAD_ERROR) }
            return
        }

        if (user.level == SpeakingLevel.NOT_ASSESSED) {
            _uiState.update {
                it.copy(
                    presentation = HomePresentation.UNASSESSED,
                    level = user.level.name,
                    displayName = user.displayName,
                    email = user.email,
                    avatarUrl = user.photoUrl
                )
            }
            return
        }

        val progress = user.trainingProgress
        if (progress == null) {
            _uiState.update { it.copy(presentation = HomePresentation.ERROR) }
            return
        }

        if (progress.stage == TrainingStage.PATH_COMPLETED) {
            val isHighestLevel = user.level == SpeakingLevel.B2
            val nextLevelStr = if (isHighestLevel) "" else getNextLevel(user.level).name

            _uiState.update {
                it.copy(
                    presentation = if (isHighestLevel) HomePresentation.HIGHEST_LEVEL_COMPLETE else HomePresentation.PATH_COMPLETE,
                    level = user.level.name,
                    nextLevel = nextLevelStr,
                    displayName = user.displayName,
                    email = user.email,
                    avatarUrl = user.photoUrl
                )
            }
            return
        }

        _uiState.update { it.copy(presentation = HomePresentation.LOADING) }
        val currentPath = trainingRepository.getTrainingPath(user.level).getOrElse { error ->
            _uiState.update { it.copy(presentation = HomePresentation.ERROR) }
            return
        }

        val currentGoal = currentPath.goals.getOrNull(progress.currentGoalOrder)
        if (currentGoal == null) {
            _uiState.update { it.copy(presentation = HomePresentation.ERROR) }
            return
        }

        val goalTitle = currentGoal.title
        val (unitTitle, unitDescription) = if (progress.stage == TrainingStage.GOAL_ASSESSMENT) {
            Pair(
                currentGoal.goalAssessment.title,
                "Hoàn thành bài kiểm tra để mở khóa mục tiêu tiếp theo."
            )
        } else {
            val currentUnit = currentGoal.units.getOrNull(progress.currentUnitOrder)
            if (currentUnit == null) {
                _uiState.update { it.copy(presentation = HomePresentation.ERROR) }
                return
            }
            Pair(
                currentUnit.title,
                currentUnit.communicativeFocus
            )
        }

        _uiState.update {
            it.copy(
                presentation = HomePresentation.ACTIVE,
                level = user.level.name,
                goalTitle = goalTitle,
                unitTitle = unitTitle,
                unitDescription = unitDescription,
                displayName = user.displayName,
                email = user.email,
                avatarUrl = user.photoUrl
            )
        }
    }

    fun retry() = observeCurrentUser()

    private fun getNextLevel(current: SpeakingLevel): SpeakingLevel {
        return when (current) {
            SpeakingLevel.A1 -> SpeakingLevel.A2
            SpeakingLevel.A2 -> SpeakingLevel.B1
            SpeakingLevel.B1 -> SpeakingLevel.B2
            else -> SpeakingLevel.B2
        }
    }
}
