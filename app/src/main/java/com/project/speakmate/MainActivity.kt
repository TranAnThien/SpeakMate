package com.project.speakmate

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.project.speakmate.ui.components.ContentErrorDialog
import com.project.speakmate.ui.call.CallRecoveryViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.app.AppNavigation
import com.project.speakmate.app.AppRoute
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.theme.SpeakMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val LOADING_USER_ID = "LOADING"
    }

    private val authViewModel: AuthViewModel by viewModels()
    private val recoveryViewModel: CallRecoveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeakMateTheme {
                val currentUserId by authViewModel.signedInUserId
                    .collectAsStateWithLifecycle(initialValue = LOADING_USER_ID)

                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val recovery by recoveryViewModel.uiState.collectAsStateWithLifecycle()
                if (currentUserId == LOADING_USER_ID || recovery.isLoading || recovery.userId != currentUserId ||
                    (currentUserId != null && authState.status == GoogleSignInStatus.LOADING)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SpeakMateLoadingIndicator(size = 40.dp)
                    }
                } else if (recovery.isNoticeVisible) {
                    BackHandler(onBack = recoveryViewModel::dismissNotice)
                    ContentErrorDialog(
                        stringResource(R.string.call_recovery_title), stringResource(R.string.call_recovery_description),
                        recoveryViewModel::dismissNotice, recoveryViewModel::dismissNotice,
                        primaryText = stringResource(R.string.call_recovery_acknowledge)
                    )
                } else {
                    val startDest = if (currentUserId != null) AppRoute.Home else AppRoute.Auth
                    key(currentUserId, recovery.wasInterrupted) {
                        AppNavigation(
                            authViewModel = authViewModel,
                            startDestination = startDest
                        )
                    }
                }
            }
        }
    }
}
