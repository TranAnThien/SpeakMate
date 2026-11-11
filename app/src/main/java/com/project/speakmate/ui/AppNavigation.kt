package com.project.speakmate.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.auth.GoogleSignInScreen
import com.project.speakmate.ui.history.HistoryScreen
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.home.HomeScreen
import com.project.speakmate.ui.home.state.HomePresentation
import com.project.speakmate.ui.home.state.HomeUiState
import com.project.speakmate.ui.practice.ScenarioCatalogScreen
import com.project.speakmate.ui.practice.state.ScenarioCatalogUiState
import com.project.speakmate.ui.placement.PlacementDestination
import com.project.speakmate.ui.placement.PlacementRoute
import com.project.speakmate.ui.placement.PlacementViewModel
import com.project.speakmate.ui.profile.ProfileScreen
import com.project.speakmate.ui.profile.ProfileViewModel
import com.project.speakmate.ui.profile.SettingsScreen
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.ui.profile.state.ProfileUiState
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.ui.training.TrainingPathScreen
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingPathUiState

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by profileViewModel.userProfile.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.signInSuccess.collect {
            navController.navigate("home") {
                popUpTo("auth") { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.forceSignOutEvent.collect {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            val context = LocalContext.current
            val webClientId = stringResource(id = R.string.default_web_client_id)

            val gso = remember {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .requestProfile()
                    .build()
            }
            val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken.isNullOrBlank()) {
                            authViewModel.onGoogleSignInError(
                                IllegalStateException("Google Sign-In returned no ID token")
                            )
                        } else {
                            authViewModel.handleGoogleSignIn(
                                idToken = idToken,
                                displayName = account.displayName,
                                email = account.email,
                                photoUrl = account.photoUrl?.toString()
                            )
                        }
                    } catch (e: ApiException) {
                        Log.e("AppNavigation", "Google sign in failed", e)
                        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            authViewModel.onGoogleSignInCancelled()
                        } else {
                            authViewModel.onGoogleSignInError(e)
                        }
                    }
                } else {
                    Log.w("AppNavigation", "Sign in canceled with code: ${result.resultCode}")
                    authViewModel.onGoogleSignInCancelled()
                }
            }

            GoogleSignInScreen(
                state = authUiState,
                onSignIn = { launcher.launch(googleSignInClient.signInIntent) },
                onOpenTerms = {},
                onOpenPrivacyPolicy = {}
            )
        }

        composable("home") {
            val homeUiState = remember(userProfile) {
                val user = userProfile
                if (user == null) {
                    HomeUiState(presentation = HomePresentation.LOADING)
                } else if (user.level == SpeakingLevel.NOT_ASSESSED) {
                    HomeUiState(
                        presentation = HomePresentation.UNASSESSED,
                        level = user.level.name,
                        displayName = user.displayName,
                        email = user.email,
                        avatarUrl = user.photoUrl
                    )
                } else {
                    HomeUiState(
                        presentation = HomePresentation.ACTIVE,
                        level = user.level.name,
                        displayName = user.displayName,
                        email = user.email,
                        avatarUrl = user.photoUrl
                    )
                }
            }

            HomeScreen(
                state = homeUiState,
                onStartAssessment = { navController.navigate(PlacementDestination.GRAPH) { launchSingleTop = true } },
                onContinuePath = { navController.navigate("home") { launchSingleTop = true } },
                onStartReassessment = {},
                onReviewPath = { navController.navigate("home") { launchSingleTop = true } },
                onSelectScenario = { navController.navigate("practice") { launchSingleTop = true } },
                onRetry = {},
                onOpenTrainingPath = { navController.navigate("home") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        navigation(startDestination = PlacementDestination.INTRODUCTION, route = PlacementDestination.GRAPH) {
            listOf(PlacementDestination.INTRODUCTION, PlacementDestination.CALL, PlacementDestination.STATUS, PlacementDestination.RESULT).forEach { destination ->
                composable(destination) { entry ->
                    val graphEntry = remember(entry) { navController.getBackStackEntry(PlacementDestination.GRAPH) }
                    val placementViewModel: PlacementViewModel = hiltViewModel(graphEntry)
                    PlacementRoute(
                        viewModel = placementViewModel,
                        currentDestination = destination,
                        onNavigate = { nextDestination ->
                            if (navController.currentBackStackEntry == entry) {
                                navController.navigate(nextDestination) {
                                    popUpTo(PlacementDestination.INTRODUCTION)
                                    launchSingleTop = true
                                }
                            }
                        },
                        onHome = {
                            if (navController.currentDestination?.parent?.route == PlacementDestination.GRAPH) {
                                navController.navigate("home") {
                                    popUpTo(PlacementDestination.GRAPH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }

        composable("training_path") {
            val trainingState = remember(userProfile) {
                TrainingPathUiState(
                    status = TrainingContentStatus.CONTENT,
                    level = userProfile?.level?.name ?: "CHƯA ĐÁNH GIÁ"
                )
            }
            TrainingPathScreen(
                state = trainingState,
                onToggleGoal = {},
                onOpenGoal = {},
                onOpenUnit = {},
                onContinuePath = {},
                onShowLevelInformation = {},
                onRetry = {},
                onOpenTrainingPath = { navController.navigate("home") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("practice") {
            ScenarioCatalogScreen(
                state = ScenarioCatalogUiState(),
                onSelectCategory = {},
                onSelectScenario = {},
                onRetry = {},
                onDismissError = {},
                onOpenTrainingPath = { navController.navigate("home") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("history") {
            HistoryScreen(
                state = HistoryUiState(),
                onSelectFilter = {},
                onOpenSession = {},
                onStartPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onRetry = {},
                onDismissError = {},
                onOpenTrainingPath = { navController.navigate("home") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("profile") {
            val profileUiState = remember(userProfile) {
                val user = userProfile

                val skillsList = if (user?.latestAssessmentCriteria != null) {
                    val criteria = user.latestAssessmentCriteria
                    listOf(
                        ProfileSkillUiState(
                            id = "range",
                            title = "Range (Từ vựng)",
                            isPassed = criteria.rangeMet,
                            description = "Sử dụng từ vựng đa dạng, linh hoạt"
                        ),
                        ProfileSkillUiState(
                            id = "accuracy",
                            title = "Accuracy (Chính xác)",
                            isPassed = criteria.accuracyMet,
                            description = "Ngữ pháp và phát âm đúng chuẩn"
                        ),
                        ProfileSkillUiState(
                            id = "fluency",
                            title = "Fluency (Trôi chảy)",
                            isPassed = criteria.fluencyMet,
                            description = "Khả năng duy trì lời nói tương đối trôi chảy"
                        ),
                        ProfileSkillUiState(
                            id = "interaction",
                            title = "Interaction (Tương tác)",
                            isPassed = criteria.interactionMet,
                            description = "Khả năng phản hồi và dẫn dắt"
                        ),
                        ProfileSkillUiState(
                            id = "coherence",
                            title = "Coherence (Gắn kết)",
                            isPassed = criteria.coherenceMet,
                            description = "Tính liên kết giữa các câu"
                        )
                    )
                } else {
                    emptyList()
                }

                ProfileUiState(
                    displayName = user?.displayName.orEmpty(),
                    email = user?.email.orEmpty(),
                    avatarUrl = user?.photoUrl,
                    levelLabel = user?.level?.name ?: "CHƯA ĐÁNH GIÁ",
                    assessmentDescription = if (user?.level == SpeakingLevel.NOT_ASSESSED) {
                        "Bạn chưa thực hiện bài đánh giá trình độ."
                    } else {
                        "Trình độ nói hiện tại của bạn."
                    },
                    skills = skillsList
                )
            }

            ProfileScreen(
                state = profileUiState,
                onOpenSettings = { navController.navigate("settings") },
                onOpenVocabulary = {},
                onOpenTrainingPath = { navController.navigate("home") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("settings") {
            var isSignOutConfirmationVisible by remember { mutableStateOf(false) }
            val context = LocalContext.current

            val settingsUiState = SettingsUiState(
                languageLabel = "Tiếng Việt",
                isSignOutConfirmationVisible = isSignOutConfirmationVisible
            )

            SettingsScreen(
                state = settingsUiState,
                onBack = { navController.popBackStack() },
                onSelectLanguage = {},
                onOpenPrivacyPolicy = {},
                onOpenTerms = {},
                onRequestSignOut = { isSignOutConfirmationVisible = true },
                onConfirmSignOut = {
                    isSignOutConfirmationVisible = false
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)

                    googleSignInClient.signOut().addOnCompleteListener {
                        profileViewModel.signOut {
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
                onCancelSignOut = { isSignOutConfirmationVisible = false }
            )
        }
    }
}
