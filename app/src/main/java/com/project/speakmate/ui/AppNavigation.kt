package com.project.speakmate.ui

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.project.speakmate.ui.history.HistoryViewModel
import com.project.speakmate.ui.history.SessionDetailScreen
import com.project.speakmate.ui.history.SessionDetailViewModel
import com.project.speakmate.ui.home.HomeScreen
import com.project.speakmate.ui.home.HomeViewModel
import com.project.speakmate.ui.home.state.HomePresentation
import com.project.speakmate.ui.practice.PracticeViewModel
import com.project.speakmate.ui.practice.ScenarioCatalogScreen
import com.project.speakmate.ui.practice.ScenarioPreparationScreen
import com.project.speakmate.ui.practice.ScenarioPreparationViewModel
import com.project.speakmate.ui.placement.ReassessmentRoute
import com.project.speakmate.ui.placement.ReassessmentViewModel
import com.project.speakmate.ui.placement.PlacementDestination
import com.project.speakmate.ui.placement.PlacementRoute
import com.project.speakmate.ui.placement.PlacementViewModel
import com.project.speakmate.ui.profile.ProfileScreen
import com.project.speakmate.ui.profile.ProfileViewModel
import com.project.speakmate.ui.profile.SettingsScreen
import com.project.speakmate.ui.profile.state.SettingsUiState
import com.project.speakmate.ui.training.TrainingPathScreen
import com.project.speakmate.ui.training.TrainingPathViewModel
import com.project.speakmate.ui.training.UnitPreparationViewModel
import com.project.speakmate.ui.training.UnitPreparationRoute
import com.project.speakmate.ui.training.UnitResultRoute
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.utils.AppConstant
import com.project.speakmate.ui.training.GoalAssessmentRoute
import com.project.speakmate.ui.training.GoalAssessmentResultRoute
import com.project.speakmate.ui.training.GoalAssessmentViewModel
import com.project.speakmate.ui.training.PathCompletionRoute
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.ui.training.GoalDetailScreen
import com.project.speakmate.ui.training.components.TrainingUnavailableContent
import com.project.speakmate.ui.training.state.GoalDetailUiState
import com.project.speakmate.ui.training.state.TrainingContentStatus
import com.project.speakmate.ui.training.state.TrainingProgressStatus
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.profile.savedvocab.SavedVocabRoute

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val openTrainingActivity: (TrainingActivityDestination) -> Unit = remember(navController) {
        { destination ->
            when (destination) {
                is TrainingActivityDestination.Unit -> navController.navigate(unitDestination(AppConstant.UNIT_PREPARATION_ROUTE, destination.unitId))
                is TrainingActivityDestination.GoalAssessment -> navController.navigate(goalDestination(AppConstant.GOAL_ASSESSMENT_ROUTE, destination.goalId))
            }
        }
    }

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

            HomeScreen(
                state = homeUiState,
                onStartAssessment = { navController.navigate(PlacementDestination.GRAPH) { launchSingleTop = true } },
                onContinuePath = { navController.navigate("training_path") { launchSingleTop = true } },
                onStartReassessment = { navController.navigate(AppConstant.REASSESSMENT_GRAPH_ROUTE) { launchSingleTop = true } },
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

        navigation(startDestination = AppConstant.REASSESSMENT_INTRODUCTION_ROUTE, route = AppConstant.REASSESSMENT_GRAPH_ROUTE) {
            listOf(AppConstant.REASSESSMENT_INTRODUCTION_ROUTE, AppConstant.REASSESSMENT_CALL_ROUTE, AppConstant.REASSESSMENT_STATUS_ROUTE, AppConstant.REASSESSMENT_RESULT_ROUTE).forEach { destination ->
                composable(destination) { entry ->
                    val graphEntry = remember(entry) { navController.getBackStackEntry(AppConstant.REASSESSMENT_GRAPH_ROUTE) }
                    val reassessmentViewModel: ReassessmentViewModel = hiltViewModel(graphEntry)
                    ReassessmentRoute(
                        viewModel = reassessmentViewModel,
                        currentDestination = destination,
                        onNavigate = { nextDestination ->
                            if (navController.currentBackStackEntry == entry) {
                                navController.navigate(nextDestination) {
                                    popUpTo(AppConstant.REASSESSMENT_INTRODUCTION_ROUTE)
                                    launchSingleTop = true
                                }
                            }
                        },
                        onReturnToPath = {
                            if (navController.currentDestination?.parent?.route == AppConstant.REASSESSMENT_GRAPH_ROUTE &&
                                !navController.popBackStack(AppConstant.PATH_COMPLETION_ROUTE, false) &&
                                !navController.popBackStack(AppConstant.TRAINING_PATH_ROUTE, false)
                            ) {
                                navController.navigate(AppConstant.TRAINING_PATH_ROUTE) {
                                    popUpTo(AppConstant.REASSESSMENT_GRAPH_ROUTE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenPromotedPath = {
                            if (navController.currentDestination?.parent?.route == AppConstant.REASSESSMENT_GRAPH_ROUTE &&
                                !navController.popBackStack(AppConstant.TRAINING_PATH_ROUTE, false)
                            ) {
                                navController.navigate(AppConstant.TRAINING_PATH_ROUTE) {
                                    popUpTo(AppConstant.REASSESSMENT_GRAPH_ROUTE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onHome = {
                            if (navController.currentDestination?.parent?.route == AppConstant.REASSESSMENT_GRAPH_ROUTE) {
                                navController.navigate("home") {
                                    popUpTo(AppConstant.REASSESSMENT_GRAPH_ROUTE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }

        composable("training_path") {
            val trainingViewModel: TrainingPathViewModel = hiltViewModel()
            val trainingState by trainingViewModel.uiState.collectAsStateWithLifecycle()

            TrainingPathScreen(
                state = trainingState,
                onToggleGoal = { goalId -> trainingViewModel.toggleGoalExpansion(goalId) },
                onOpenGoal = { goalId -> navController.navigate(goalDestination(AppConstant.GOAL_DETAIL_ROUTE, goalId)) },
                onOpenUnit = openTrainingActivity,
                onContinuePath = {
                    trainingState.goals.asSequence()
                        .flatMap { it.units.asSequence() }
                        .firstOrNull { it.status == TrainingProgressStatus.CURRENT && it.canOpen }
                        ?.destination
                        ?.let(openTrainingActivity)
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

        composable(AppConstant.GOAL_ASSESSMENT_ROUTE) { entry ->
            val goalId = entry.arguments?.getString(AppConstant.GOAL_ID_ARGUMENT).orEmpty()
            GoalAssessmentRoute(
                onBack = navController::popBackStack,
                onStartAssessment = {
                    if (navController.currentBackStackEntry == entry) {
                        navController.navigate(goalDestination(AppConstant.GOAL_ASSESSMENT_CALL_ROUTE, goalId))
                    }
                },
                onReviewLessons = { navController.navigate(goalDestination(AppConstant.GOAL_DETAIL_ROUTE, goalId)) }
            )
        }

        composable(AppConstant.GOAL_ASSESSMENT_CALL_ROUTE) { entry ->
            val goalId = entry.arguments?.getString(AppConstant.GOAL_ID_ARGUMENT).orEmpty()
            val introductionRoute = goalDestination(AppConstant.GOAL_ASSESSMENT_ROUTE, goalId)
            val introductionEntry = remember(entry, goalId) {
                try {
                    navController.getBackStackEntry(introductionRoute)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            if (introductionEntry == null) {
                LaunchedEffect(entry) {
                    navController.navigate(introductionRoute) {
                        popUpTo(AppConstant.GOAL_ASSESSMENT_CALL_ROUTE) { inclusive = true }
                    }
                }
            } else {
                val assessmentViewModel: GoalAssessmentViewModel = hiltViewModel(introductionEntry)
                val assessmentState by assessmentViewModel.uiState.collectAsStateWithLifecycle()
                var activity by remember(entry) { mutableStateOf<TrainingActivityScenario?>(null) }
                var isReplay by rememberSaveable { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(assessmentState.activity) {
                    if (activity == null && assessmentState.status == TrainingContentStatus.CONTENT) {
                        activity = assessmentState.activity
                        if (isReplay == null) isReplay = assessmentState.isReplay
                    }
                }
                val callActivity = activity
                if (callActivity == null) {
                    TrainingUnavailableContent(
                        isLoading = assessmentState.status == TrainingContentStatus.LOADING,
                        onRetry = navController::popBackStack
                    )
                } else {
                    CallRoute(
                        scenario = callActivity.scenario,
                        activityType = CallActivityType.GOAL_ASSESSMENT,
                        onExit = { navController.popBackStack() },
                        activityContext = CallActivityContext.Training(callActivity),
                        onPracticeCompleted = { attemptId ->
                            if (navController.currentBackStackEntry == entry) {
                                val resultRoute = goalDestination(AppConstant.GOAL_ASSESSMENT_RESULT_ROUTE, goalId)
                                    .replace("{" + AppConstant.ATTEMPT_ID_ARGUMENT + "}", Uri.encode(attemptId))
                                    .replace("{" + AppConstant.IS_REPLAY_ARGUMENT + "}", isReplay.toString())
                                navController.navigate(resultRoute) {
                                    popUpTo(AppConstant.GOAL_ASSESSMENT_CALL_ROUTE) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }

        composable(AppConstant.GOAL_ASSESSMENT_RESULT_ROUTE) { entry ->
            val goalId = entry.arguments?.getString(AppConstant.GOAL_ID_ARGUMENT).orEmpty()
            val introductionRoute = goalDestination(AppConstant.GOAL_ASSESSMENT_ROUTE, goalId)
            val introductionEntry = remember(entry, goalId) {
                try {
                    navController.getBackStackEntry(introductionRoute)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            val assessmentViewModel: GoalAssessmentViewModel? = if (introductionEntry != null) hiltViewModel(introductionEntry) else null
            val returnToPath: () -> Unit = {
                if (!navController.popBackStack(AppConstant.TRAINING_PATH_ROUTE, false)) {
                    navController.navigate(AppConstant.TRAINING_PATH_ROUTE) {
                        popUpTo(AppConstant.GOAL_ASSESSMENT_ROUTE) { inclusive = true }
                    }
                }
            }
            GoalAssessmentResultRoute(
                onBack = navController::popBackStack,
                onRetryAssessment = {
                    if (assessmentViewModel?.uiState?.value?.status == TrainingContentStatus.CONTENT) {
                        assessmentViewModel.prepareRetry()
                        navController.navigate(goalDestination(AppConstant.GOAL_ASSESSMENT_CALL_ROUTE, goalId)) {
                            popUpTo(AppConstant.GOAL_ASSESSMENT_ROUTE) { inclusive = false }
                        }
                    } else if (introductionEntry != null) {
                        navController.popBackStack(introductionRoute, false)
                    } else {
                        navController.navigate(introductionRoute) {
                            popUpTo(AppConstant.GOAL_ASSESSMENT_RESULT_ROUTE) { inclusive = true }
                        }
                    }
                },
                onOpenNextGoal = { nextGoalId ->
                    navController.navigate(goalDestination(AppConstant.GOAL_DETAIL_ROUTE, nextGoalId)) {
                        popUpTo(AppConstant.GOAL_ASSESSMENT_ROUTE) { inclusive = true }
                    }
                },
                onViewCompletedPath = {
                    navController.navigate(AppConstant.PATH_COMPLETION_ROUTE) {
                        popUpTo(AppConstant.GOAL_ASSESSMENT_ROUTE) { inclusive = true }
                    }
                },
                onReturnToPath = returnToPath,
                onReviewLessons = {
                    navController.navigate(goalDestination(AppConstant.GOAL_DETAIL_ROUTE, goalId)) {
                        popUpTo(AppConstant.GOAL_ASSESSMENT_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppConstant.PATH_COMPLETION_ROUTE) {
            val returnToPath: () -> Unit = {
                if (!navController.popBackStack(AppConstant.TRAINING_PATH_ROUTE, false)) {
                    navController.navigate(AppConstant.TRAINING_PATH_ROUTE) {
                        popUpTo(AppConstant.PATH_COMPLETION_ROUTE) { inclusive = true }
                    }
                }
            }
            PathCompletionRoute(
                onBack = navController::popBackStack,
                onReturnToPath = returnToPath,
                onStartPromotion = { navController.navigate(AppConstant.REASSESSMENT_GRAPH_ROUTE) { launchSingleTop = true } },
                onFreePractice = {
                    navController.navigate("practice") {
                        popUpTo(AppConstant.PATH_COMPLETION_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppConstant.UNIT_PREPARATION_ROUTE) { entry ->
            val unitId = entry.arguments?.getString(AppConstant.UNIT_ID_ARGUMENT).orEmpty()
            UnitPreparationRoute(
                unitId = unitId,
                onBack = navController::popBackStack,
                onStartRoleplay = { navController.navigate(unitDestination(AppConstant.UNIT_CALL_ROUTE, unitId)) }
            )
        }

        composable(AppConstant.UNIT_CALL_ROUTE) { entry ->
            val unitId = entry.arguments?.getString(AppConstant.UNIT_ID_ARGUMENT).orEmpty()
            val preparationRoute = unitDestination(AppConstant.UNIT_PREPARATION_ROUTE, unitId)
            val unitEntry = remember(entry, unitId) {
                try {
                    navController.getBackStackEntry(preparationRoute)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            if (unitEntry == null) {
                LaunchedEffect(entry, preparationRoute) {
                    navController.navigate(preparationRoute) {
                        popUpTo(AppConstant.UNIT_CALL_ROUTE) { inclusive = true }
                    }
                }
            } else {
                val preparationViewModel: UnitPreparationViewModel = hiltViewModel(unitEntry)
                val preparationState by preparationViewModel.uiState.collectAsStateWithLifecycle()
                val activity = remember(entry) { preparationState.activity }
                if (activity == null) {
                    TrainingUnavailableContent(isLoading = false, onRetry = { navController.popBackStack() })
                } else {
                    CallRoute(
                        scenario = activity.scenario,
                        activityType = CallActivityType.TRAINING_UNIT,
                        onExit = { navController.popBackStack() },
                        activityContext = CallActivityContext.Training(activity),
                        onPracticeCompleted = { attemptId ->
                            val resultRoute = unitDestination(AppConstant.UNIT_RESULT_ROUTE, unitId)
                                .replace("{"+AppConstant.ATTEMPT_ID_ARGUMENT+"}", Uri.encode(attemptId))
                            navController.navigate(resultRoute) {
                                popUpTo(AppConstant.UNIT_CALL_ROUTE) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

        composable(AppConstant.UNIT_RESULT_ROUTE) { entry ->
            val unitId = entry.arguments?.getString(AppConstant.UNIT_ID_ARGUMENT).orEmpty()
            val preparationRoute = unitDestination(AppConstant.UNIT_PREPARATION_ROUTE, unitId)
            val unitEntry = remember(entry, unitId) {
                try {
                    navController.getBackStackEntry(preparationRoute)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            val preparationViewModel: UnitPreparationViewModel? = if (unitEntry != null) hiltViewModel(unitEntry) else null
            val retryUnit: () -> Unit = {
                if (preparationViewModel?.uiState?.value?.canStartRoleplay == true) {
                    preparationViewModel.prepareRetry()
                    preparationViewModel.stopPronunciation()
                    navController.navigate(unitDestination(AppConstant.UNIT_CALL_ROUTE, unitId)) {
                        popUpTo(AppConstant.UNIT_PREPARATION_ROUTE) { inclusive = false }
                    }
                } else if (unitEntry != null) {
                    navController.popBackStack(preparationRoute, false)
                } else {
                    navController.navigate(preparationRoute) {
                        popUpTo(AppConstant.UNIT_RESULT_ROUTE) { inclusive = true }
                    }
                }
            }
            val returnToPath = {
                if (!navController.popBackStack(AppConstant.TRAINING_PATH_ROUTE, false)) {
                    navController.navigate(AppConstant.TRAINING_PATH_ROUTE)
                }
            }
            UnitResultRoute(
                onBack = navController::popBackStack,
                onContinuePath = returnToPath,
                onReplayUnit = retryUnit,
                onRetryUnit = retryUnit,
                onReviewLesson = { navController.popBackStack() }
            )
        }

        composable(AppConstant.GOAL_DETAIL_ROUTE) { entry ->
            val goalId = entry.arguments?.getString(AppConstant.GOAL_ID_ARGUMENT).orEmpty()
            val pathEntry = remember(entry) {
                try {
                    navController.getBackStackEntry(AppConstant.TRAINING_PATH_ROUTE)
                } catch (_: IllegalArgumentException) {
                    entry
                }
            }
            val trainingViewModel: TrainingPathViewModel = hiltViewModel(pathEntry)
            val trainingState by trainingViewModel.uiState.collectAsStateWithLifecycle()
            val goal = trainingState.goals.firstOrNull { it.id == goalId }
            val detailState = goal?.let {
                GoalDetailUiState(
                    title = it.title,
                    description = it.summary,
                    units = it.units,
                    canPrepareCurrentUnit = it.units.any { unit ->
                        unit.status == TrainingProgressStatus.CURRENT && unit.canOpen
                    }
                )
            } ?: GoalDetailUiState()
            GoalDetailScreen(
                state = detailState,
                onBack = navController::popBackStack,
                onOpenUnit = openTrainingActivity,
                onPrepareCurrentUnit = {
                    goal?.units?.firstOrNull { unit ->
                        unit.status == TrainingProgressStatus.CURRENT && unit.canOpen
                    }?.destination?.let(openTrainingActivity)
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
            val viewModel: HistoryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state,
                onSelectFilter = viewModel::onSelectFilter,
                onOpenSession = { sessionId ->
                    navController.navigate("session_detail/$sessionId")
                },
                onStartPractice = {
                    if (state.selectedFilterId == "free_practice") {
                        navController.navigate("practice") { launchSingleTop = true }
                    } else {
                        navController.navigate("training_path") { launchSingleTop = true }
                    }
                },
                onRetry = viewModel::retry,
                onDismissError = {},
                onOpenTrainingPath = { navController.navigate("training_path") { launchSingleTop = true } },
                onOpenPractice = { navController.navigate("practice") { launchSingleTop = true } },
                onOpenHistory = {},
                onOpenProfile = { navController.navigate("profile") { launchSingleTop = true } }
            )
        }

        composable(
            route = "session_detail/{attemptId}",
            arguments = listOf(navArgument("attemptId") { type = NavType.StringType })
        ) {
            val viewModel: SessionDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SessionDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRetrySession = viewModel::loadSessionDetail,
                onRetryTranscript = viewModel::loadSessionDetail,
                onDismissError = { navController.popBackStack() },
                onReturnToHistory = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                state = profileUiState,
                onOpenSettings = { navController.navigate("settings") },
                onOpenVocabulary = { navController.navigate("saved_vocab") { launchSingleTop = true } },
                onRetryProfile = profileViewModel::retryProfile,
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
            SavedVocabRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


private fun unitDestination(route: String, unitId: String): String =
    route.replace("{"+AppConstant.UNIT_ID_ARGUMENT+"}", Uri.encode(unitId))

private fun goalDestination(route: String, goalId: String): String =
    route.replace("{" + AppConstant.GOAL_ID_ARGUMENT + "}", Uri.encode(goalId))
