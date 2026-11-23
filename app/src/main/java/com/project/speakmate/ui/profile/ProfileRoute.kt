package com.project.speakmate.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.project.speakmate.ui.profile.state.ProfileEffect

@Composable
fun ProfileRoute(
    onOpenSettings: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onSignedOut: () -> Unit,
    onStartPlacement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    ProfileEffect.SignedOut -> signedOut()
                }
            }
        }
    }
    ProfileScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenVocabulary = onOpenVocabulary,
        onRetryProfile = viewModel::retryProfile,
        onStartPlacement = onStartPlacement,
        modifier = modifier
    )
}
