package com.project.speakmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.data.remote.firebase.FirestoreUserDataSource
import com.project.speakmate.data.repository.AuthRepositoryImpl
import com.project.speakmate.data.repository.UserRepositoryImpl
import com.project.speakmate.ui.AppNavigation
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.profile.ProfileViewModel
import com.project.speakmate.ui.theme.SpeakMateTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory = object : ViewModelProvider.Factory {
        val authRepo = AuthRepositoryImpl(FirebaseAuthDataSource())
        val userRepo = UserRepositoryImpl(FirestoreUserDataSource())

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepo, userRepo) as T
            }
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(authRepo, userRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeakMateTheme {
                val currentUserId by viewModelFactory.authRepo.getSignedInUserId()
                    .collectAsState(initial = "LOADING")

                if (currentUserId == "LOADING") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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