package com.project.speakmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.data.repository.AuthRepositoryImpl
import com.project.speakmate.data.repository.UserRepositoryImpl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speakmate.ui.AppNavigation
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.profile.ProfileViewModel
import com.project.speakmate.ui.theme.SpeakMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val LOADING_USER_ID = "LOADING"
    }

    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeakMateTheme {
                val currentUserId by authViewModel.signedInUserId
                    .collectAsStateWithLifecycle(initialValue = LOADING_USER_ID)

                if (currentUserId == LOADING_USER_ID) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SpeakMateLoadingIndicator(size = 40.dp)
                    }
                } else {
                    val startDest = if (currentUserId != null) "home" else "auth"
                    AppNavigation(
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        startDestination = startDest
                    )
                }
            }
        }
    }
}
