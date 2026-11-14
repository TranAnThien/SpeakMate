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
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.auth.GoogleSignInScreen
import com.project.speakmate.ui.history.HistoryScreen
import com.project.speakmate.ui.history.state.HistoryUiState
import com.project.speakmate.ui.home.HomeScreen
import com.project.speakmate.ui.home.HomeViewModel
import com.project.speakmate.ui.practice.PracticeViewModel
import com.project.speakmate.ui.practice.ScenarioCatalogScreen
import com.project.speakmate.ui.practice.ScenarioPreparationScreen
import com.project.speakmate.ui.practice.ScenarioPreparationViewModel
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
import com.project.speakmate.ui.training.TrainingViewModel
import com.project.speakmate.ui.training.UnitPreparationScreen
import com.project.speakmate.ui.training.GoalDetailScreen
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.profile.savedvocab.SavedVocabScreen

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

            @Suppress("DEPRECATION")
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
            val homeViewModel: HomeViewModel = hiltViewModel()
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(userProfile) {
                homeViewModel.updateUserData(userProfile)
            }

            HomeScreen(
                state = homeUiState,
                onStartAssessment = { navController.navigate(PlacementDestination.GRAPH) { launchSingleTop = true } },
                onContinuePath = { navController.navigate("training_path") { launchSingleTop = true } },
                onStartReassessment = {},
                onReviewPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onSelectScenario = { navController.navigate("practice") { launchSingleTop = true } },
                onRetry = homeViewModel::retry,
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
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
            val trainingViewModel: TrainingViewModel = hiltViewModel()
            val trainingState by trainingViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(userProfile) {
                trainingViewModel.updateUserData(userProfile)
            }

            TrainingPathScreen(
                state = trainingState,
                onToggleGoal = { goalId -> trainingViewModel.toggleGoalExpansion(goalId) },
                onOpenGoal = { goalId -> navController.navigate("training_goal/$goalId") },
                onOpenUnit = { unitId ->
                    if (!unitId.startsWith("assessment:")) navController.navigate("training_unit/$unitId")
                },
                onContinuePath = {
                    trainingState.goals.asSequence()
                        .flatMap { it.units.asSequence() }
                        .firstOrNull { it.status == TrainingProgressStatus.CURRENT && !it.isAssessment }
                        ?.id
                        ?.let { unitId -> navController.navigate("training_unit/$unitId") }
                },
                onShowLevelInformation = {},
                onRetry = trainingViewModel::retry,
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("practice") {
            val practiceViewModel: PracticeViewModel = hiltViewModel()
            val practiceState by practiceViewModel.uiState.collectAsStateWithLifecycle()

            ScenarioCatalogScreen(
                state = practiceState,
                onSelectCategory = { categoryId -> practiceViewModel.onCategorySelected(categoryId) },
                onSelectScenario = { practiceId -> navController.navigate("practice_preparation/$practiceId") },
                onRetry = practiceViewModel::loadCatalog,
                onDismissError = {},
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("training_unit/{unitId}") { entry ->
            val unitId = entry.arguments?.getString("unitId").orEmpty()
            val trainingViewModel: TrainingViewModel = hiltViewModel()
            val preparationState by trainingViewModel.preparationState.collectAsStateWithLifecycle()
            LaunchedEffect(unitId) { trainingViewModel.loadUnitPreparation(unitId) }
            when (preparationState.status) {
                TrainingContentStatus.CONTENT -> UnitPreparationScreen(
                    state = preparationState,
                    onBack = navController::popBackStack,
                    onSelectSection = trainingViewModel::selectPreparationSection,
                    onStartRoleplay = {},
                    onPlayWord = {},
                    onPlayPattern = {},
                    onPlayExample = {}
                )
                TrainingContentStatus.LOADING -> TrainingUnavailableContent(
                    isLoading = true,
                    onRetry = { trainingViewModel.loadUnitPreparation(unitId) },
                    loadingTitleRes = R.string.preparation_loading_title,
                    loadingDescriptionRes = R.string.preparation_loading_description,
                    errorTitleRes = R.string.preparation_error_title,
                    errorDescriptionRes = R.string.preparation_error_description
                )
                else -> TrainingUnavailableContent(
                    isLoading = false,
                    onRetry = { trainingViewModel.loadUnitPreparation(unitId) },
                    errorMessage = preparationState.errorMessage,
                    loadingTitleRes = R.string.preparation_loading_title,
                    loadingDescriptionRes = R.string.preparation_loading_description,
                    errorTitleRes = R.string.preparation_error_title,
                    errorDescriptionRes = R.string.preparation_error_description
                )
            }
        }

        composable("training_goal/{goalId}") { entry ->
            val goalId = entry.arguments?.getString("goalId").orEmpty()
            val pathEntry = remember(entry) { navController.getBackStackEntry("training_path") }
            val trainingViewModel: TrainingViewModel = hiltViewModel(pathEntry)
            val trainingState by trainingViewModel.uiState.collectAsStateWithLifecycle()
            val goal = trainingState.goals.firstOrNull { it.id == goalId }
            val detailState = goal?.let {
                GoalDetailUiState(
                    title = it.title,
                    description = it.summary,
                    units = it.units,
                    canPrepareCurrentUnit = it.units.any { unit ->
                        unit.status == TrainingProgressStatus.CURRENT && !unit.isAssessment
                    }
                )
            } ?: GoalDetailUiState()
            GoalDetailScreen(
                state = detailState,
                onBack = navController::popBackStack,
                onOpenUnit = { unitId ->
                    if (!unitId.startsWith("assessment:")) navController.navigate("training_unit/$unitId")
                },
                onPrepareCurrentUnit = {
                    goal?.units?.firstOrNull { unit ->
                        unit.status == TrainingProgressStatus.CURRENT && !unit.isAssessment
                    }?.id?.let { unitId -> navController.navigate("training_unit/$unitId") }
                },
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("practice_preparation/{practiceId}") { entry ->
            val practiceId = entry.arguments?.getString("practiceId").orEmpty()
            val preparationViewModel: ScenarioPreparationViewModel = hiltViewModel()
            val preparationState by preparationViewModel.uiState.collectAsStateWithLifecycle()
            ScenarioPreparationScreen(
                state = preparationState,
                onBack = navController::popBackStack,
                onStartCall = {
                    preparationViewModel.onStartCallPressed()
                    navController.navigate("free_practice_call/$practiceId")
                },
                onRetry = preparationViewModel::loadScenarioDetails
            )
        }

        composable("free_practice_call/{practiceId}") { entry ->
            val practiceId = entry.arguments?.getString("practiceId").orEmpty()

            val preparationEntry = remember(entry, practiceId) {
                navController.getBackStackEntry("practice_preparation/$practiceId")
            }
            val preparationViewModel: ScenarioPreparationViewModel = hiltViewModel(preparationEntry)
            val preparationState by preparationViewModel.uiState.collectAsStateWithLifecycle()
            val scenario = preparationState.scenario
            if (scenario == null) {
                ScenarioPreparationScreen(
                    state = preparationState,
                    onBack = {
                        preparationViewModel.onCallExited()
                        navController.popBackStack()
                    },
                    onStartCall = {},
                    onRetry = preparationViewModel::loadScenarioDetails
                )
            } else {
                CallRoute(
                    scenario = scenario,
                    activityType = CallActivityType.FREE_PRACTICE,
                    onExit = {
                        preparationViewModel.onCallExited()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("history") {
            HistoryScreen(
                state = HistoryUiState(),
                onSelectFilter = {},
                onOpenSession = {},
                onStartPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onRetry = {},
                onDismissError = {},
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
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
                        ProfileSkillUiState(id = "range", title = "Range (Từ vựng)", isPassed = criteria.rangeMet, description = "Sử dụng từ vựng đa dạng"),
                        ProfileSkillUiState(id = "accuracy", title = "Accuracy (Chính xác)", isPassed = criteria.accuracyMet, description = "Ngữ pháp và phát âm chuẩn"),
                        ProfileSkillUiState(id = "fluency", title = "Fluency (Trôi chảy)", isPassed = criteria.fluencyMet, description = "Khả năng duy trì lời nói"),
                        ProfileSkillUiState(id = "interaction", title = "Interaction (Tương tác)", isPassed = criteria.interactionMet, description = "Khả năng phản hồi"),
                        ProfileSkillUiState(id = "coherence", title = "Coherence (Gắn kết)", isPassed = criteria.coherenceMet, description = "Tính liên kết giữa các câu")
                    )
                } else emptyList()

                ProfileUiState(
                    displayName = user?.displayName.orEmpty(),
                    email = user?.email.orEmpty(),
                    avatarUrl = user?.photoUrl,
                    levelLabel = user?.level?.name ?: "CHƯA ĐÁNH GIÁ",
                    assessmentDescription = if (user?.level == SpeakingLevel.NOT_ASSESSED) "Bạn chưa thực hiện bài đánh giá trình độ." else "Trình độ nói hiện tại của bạn.",
                    skills = skillsList
                )
            }

            ProfileScreen(
                state = profileUiState,
                onOpenSettings = { navController.navigate("settings") },
                onOpenVocabulary = { navController.navigate("saved_vocab") { launchSingleTop = true } },
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = { navController.navigate("history") { launchSingleTop = true } },
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable("settings") {
            var isSignOutConfirmationVisible by remember { mutableStateOf(false) }
            val context = LocalContext.current

            SettingsScreen(
                state = SettingsUiState(languageLabel = "Tiếng Việt", isSignOutConfirmationVisible = isSignOutConfirmationVisible),
                onBack = { navController.popBackStack() },
                onSelectLanguage = {},
                onOpenPrivacyPolicy = {},
                onOpenTerms = {},
                onRequestSignOut = { isSignOutConfirmationVisible = true },
                onConfirmSignOut = {
                    isSignOutConfirmationVisible = false
                    @Suppress("DEPRECATION")
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                        profileViewModel.signOut {
                            navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                        }
                    }
                },
                onCancelSignOut = { isSignOutConfirmationVisible = false }
            )
        }

        composable("saved_vocab") {
            SavedVocabScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
