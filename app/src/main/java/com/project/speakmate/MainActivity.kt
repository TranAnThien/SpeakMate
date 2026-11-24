package com.project.speakmate

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.app.AppLocaleProvider
import com.project.speakmate.app.AppNavigation
import com.project.speakmate.app.AppRoute
import com.project.speakmate.app.SpeakMateWindowContent
import com.project.speakmate.app.hideSpeakMateSystemBars
import com.project.speakmate.domain.repository.PracticeCompletionRepository
import com.project.speakmate.ui.auth.AuthRoute
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.call.CallRecoveryViewModel
import com.project.speakmate.ui.components.dialog.ContentErrorDialog
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.SpeakMateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val LOADING_USER_ID = "LOADING"
    }

    @Inject lateinit var completionRepository: Provider<PracticeCompletionRepository>

    private val authViewModel: AuthViewModel by viewModels()
    private val recoveryViewModel: CallRecoveryViewModel by viewModels()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleProvider().wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        hideSpeakMateSystemBars(window)
        setContent {
            SpeakMateTheme {
                SpeakMateWindowContent {
                    val currentUserId by
                        authViewModel.signedInUserId.collectAsStateWithLifecycle(initialValue = LOADING_USER_ID)

                    val sessionUserId = currentUserId
                    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                    val recovery by recoveryViewModel.uiState.collectAsStateWithLifecycle()
                    if (sessionUserId == LOADING_USER_ID || recovery.isLoading || recovery.userId != sessionUserId) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SpeakMateLoadingIndicator(size = 40.dp)
                        }
                    } else if (recovery.isNoticeVisible) {
                        BackHandler(onBack = recoveryViewModel::dismissNotice)
                        ContentErrorDialog(
                            title = stringResource(R.string.call_recovery_title),
                            description = stringResource(R.string.call_recovery_description),
                            onRetry = recoveryViewModel::dismissNotice,
                            onDismiss = recoveryViewModel::dismissNotice,
                            primaryText = stringResource(R.string.call_recovery_acknowledge)
                        )
                    } else if (sessionUserId == null ||
                        !authState.isProfileReady ||
                        authState.userId != sessionUserId) {
                        AuthRoute(authViewModel)
                    } else {
                        val startDest = AppRoute.TrainingPath()
                        key(sessionUserId, recovery.wasInterrupted) {
                            AppNavigation(
                                authViewModel = authViewModel,
                                sessionUserId = sessionUserId,
                                startDestination = startDest
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSpeakMateSystemBars(window)
    }

    override fun onStart() {
        super.onStart()
        completionRepository.get().resumePendingSaves()
    }
}
