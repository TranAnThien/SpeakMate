package com.project.speakmate.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.project.speakmate.R
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.call.components.CallExitNoticeCard
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
import com.project.speakmate.ui.history.HistoryRoute
import com.project.speakmate.ui.history.SessionDetailRoute
import com.project.speakmate.ui.placement.PlacementRoute
import com.project.speakmate.ui.placement.ReassessmentRoute
import com.project.speakmate.ui.practice.PracticeSummaryRoute
import com.project.speakmate.ui.practice.ScenarioCatalogRoute
import com.project.speakmate.ui.practice.ScenarioPreparationRoute
import com.project.speakmate.ui.profile.ProfileRoute
import com.project.speakmate.ui.profile.SettingsRoute
import com.project.speakmate.ui.profile.savedvocab.SavedVocabRoute
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.GoalAssessmentCallRoute
import com.project.speakmate.ui.training.GoalAssessmentResultRoute
import com.project.speakmate.ui.training.GoalAssessmentRoute
import com.project.speakmate.ui.training.PathCompletionRoute
import com.project.speakmate.ui.training.TrainingPathRoute
import com.project.speakmate.ui.training.UnitCallRoute
import com.project.speakmate.ui.training.UnitPreparationRoute
import com.project.speakmate.ui.training.UnitResultRoute
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    startDestination: AppRoute,
    sessionUserId: String,
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val cancellationNoticeHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var cancellationNoticeEntryId by remember { mutableStateOf<String?>(null) }
    val cancellationMessage = stringResource(R.string.call_cancelled_not_saved)
    val showCancelledCallNotice = remember(navController, cancellationNoticeHost, scope, cancellationMessage) {
        {
            cancellationNoticeEntryId = navController.currentBackStackEntry?.id
            scope.launch {
                cancellationNoticeHost.currentSnackbarData?.dismiss()
                cancellationNoticeHost.showSnackbar(cancellationMessage, duration = SnackbarDuration.Short)
            }
            Unit
        }
    }
    LaunchedEffect(entry, sessionUserId) {
        if (entry?.id != cancellationNoticeEntryId) {
            cancellationNoticeHost.currentSnackbarData?.dismiss()
            cancellationNoticeEntryId = null
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { cancellationNoticeHost.currentSnackbarData?.dismiss() }
    val destination = entry?.destination
    val selectedTab = when {
        destination?.hasRoute<AppRoute.Home>() == true || destination?.hasRoute<AppRoute.TrainingPath>() == true -> AppDestination.TRAINING
        destination?.hasRoute<AppRoute.ScenarioCatalog>() == true -> AppDestination.PRACTICE
        destination?.hasRoute<AppRoute.History>() == true -> AppDestination.HISTORY
        destination?.hasRoute<AppRoute.Profile>() == true -> AppDestination.PROFILE
        else -> null
    }
    val openPath = remember(navController) { { navigateTab(navController, AppRoute.TrainingPath()) } }
    val openPractice = remember(navController) { { navigateTab(navController, AppRoute.ScenarioCatalog) } }
    val returnToPath = remember(navController) {
        {
            if (!navController.popBackStack<AppRoute.TrainingPath>(inclusive = false)) {
                navController.navigate(AppRoute.TrainingPath()) {
                    popUpTo(navController.graph.id)
                    launchSingleTop = true
                }
            }
            Unit
        }
    }
    val openPlacement = remember(navController, authViewModel, sessionUserId) {
        {
            if (authViewModel.isSessionReady(sessionUserId)) navController.navigate(AppRoute.Placement) { launchSingleTop = true }
        }
    }
    val returnHome = returnToPath
    val openActivity: (TrainingActivityDestination) -> Unit = remember(navController, authViewModel, sessionUserId) {
        { activity ->
            if (authViewModel.isSessionReady(sessionUserId)) when (activity) {
                is TrainingActivityDestination.Unit -> navController.navigate(AppRoute.UnitPreparation(activity.unitId)) { launchSingleTop = true }
                is TrainingActivityDestination.GoalAssessment -> navController.navigate(AppRoute.GoalAssessment(activity.goalId)) { launchSingleTop = true }
            }
        }
    }
    Scaffold(
        containerColor = DesignColor.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selectedTab != null) {
                AppBottomBar(
                    onSelectDestination = { tab ->
                        val route = when (tab) {
                            AppDestination.TRAINING -> AppRoute.TrainingPath()
                            AppDestination.PRACTICE -> AppRoute.ScenarioCatalog
                            AppDestination.HISTORY -> AppRoute.History
                            AppDestination.PROFILE -> AppRoute.Profile
                        }
                        navigateTab(navController, route)
                    },
                    selectedDestination = selectedTab,
                    bottomPadding = if (selectedTab == AppDestination.TRAINING) 12.dp else 4.dp
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
        NavHost(navController, startDestination, Modifier.weight(1f)) {
            composable<AppRoute.Auth> {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.TrainingPath()) {
                        popUpTo<AppRoute.Auth> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable<AppRoute.Home> {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.TrainingPath()) {
                        popUpTo<AppRoute.Home> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            navigation<AppRoute.Placement>(startDestination = AppRoute.PlacementStep()) {
                composable<AppRoute.PlacementStep> { backStackEntry ->
                    val graphEntry = remember(backStackEntry) {
                        try { navController.getBackStackEntry<AppRoute.Placement>() } catch (_: IllegalArgumentException) { null }
                    }
                    val graphLifecycleState = graphEntry?.lifecycle?.currentStateAsState()?.value
                    if (graphEntry == null || graphLifecycleState == Lifecycle.State.DESTROYED) {
                        LaunchedEffect(backStackEntry) {
                            if (navController.currentBackStackEntry == backStackEntry) returnHome()
                        }
                    } else {
                        PlacementRoute(
                            viewModel = hiltViewModel(graphEntry),
                            isCallDestination = backStackEntry.toRoute<AppRoute.PlacementStep>().isCalling,
                            onChangeCallDestination = { isCalling ->
                                if (navController.currentBackStackEntry == backStackEntry) {
                                    navController.navigate(AppRoute.PlacementStep(isCalling)) {
                                        popUpTo<AppRoute.PlacementStep> { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onHome = returnHome
                        )
                    }
                }
            }
            navigation<AppRoute.Reassessment>(startDestination = AppRoute.ReassessmentStep()) {
                composable<AppRoute.ReassessmentStep> { backStackEntry ->
                    val graphEntry = remember(backStackEntry) {
                        try { navController.getBackStackEntry<AppRoute.Reassessment>() } catch (_: IllegalArgumentException) { null }
                    }
                    val graphLifecycleState = graphEntry?.lifecycle?.currentStateAsState()?.value
                    if (graphEntry == null || graphLifecycleState == Lifecycle.State.DESTROYED) {
                        LaunchedEffect(backStackEntry) {
                            if (navController.currentBackStackEntry == backStackEntry) returnToPath()
                        }
                    } else {
                        ReassessmentRoute(
                            viewModel = hiltViewModel(graphEntry),
                            isCallDestination = backStackEntry.toRoute<AppRoute.ReassessmentStep>().isCalling,
                            onChangeCallDestination = { isCalling ->
                                if (navController.currentBackStackEntry == backStackEntry) {
                                    navController.navigate(AppRoute.ReassessmentStep(isCalling)) {
                                        popUpTo<AppRoute.ReassessmentStep> { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onReturnToPath = returnToPath,
                            onOpenPromotedPath = returnToPath,
                            onHome = returnHome,
                            onOpenProfile = {
                                if (navController.currentBackStackEntry == backStackEntry) navController.navigate(AppRoute.Profile) {
                                    popUpTo<AppRoute.Reassessment> { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
            composable<AppRoute.TrainingPath> { pathBackStackEntry ->
                TrainingPathRoute(
                    goalId = pathBackStackEntry.toRoute<AppRoute.TrainingPath>().goalId,
                    onOpenActivity = { activity -> if (navController.currentBackStackEntry == pathBackStackEntry) openActivity(activity) },
                    onStartPlacement = openPlacement,
                    onFreePractice = openPractice,
                    onViewCompletedPath = { navController.navigate(AppRoute.PathCompletion) { launchSingleTop = true } }
                )
            }
            composable<AppRoute.UnitPreparation> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.UnitPreparation>()
                UnitPreparationRoute(route.unitId, navController::popBackStack, {
                    navController.navigate(AppRoute.UnitCall(route.unitId)) { launchSingleTop = true }
                }, noticeContent = {
                    if (cancellationNoticeEntryId == backStackEntry.id) SnackbarHost(cancellationNoticeHost) { data ->
                        CallExitNoticeCard(data.visuals.message)
                    }
                })
            }
            composable<AppRoute.UnitCall> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.UnitCall>()
                val preparationEntry = remember(backStackEntry) {
                    navController.previousBackStackEntry?.takeIf {
                        !route.isRetry && it.destination.hasRoute<AppRoute.UnitPreparation>() &&
                            it.toRoute<AppRoute.UnitPreparation>().unitId == route.unitId
                    }
                }
                UnitCallRoute(route.unitId, route.isRetry, navController::popBackStack, { attemptId, isReplay ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(AppRoute.UnitResult(route.unitId, attemptId, isReplay)) {
                            popUpTo<AppRoute.UnitCall> { inclusive = true }
                        }
                    }
                }, viewModel = hiltViewModel(preparationEntry ?: backStackEntry), onCallCancelled = {
                    if (navController.currentBackStackEntry == backStackEntry && authViewModel.isSessionReady(sessionUserId)) {
                        val previousEntry = navController.previousBackStackEntry
                        if (previousEntry?.destination?.hasRoute<AppRoute.UnitPreparation>() == true &&
                            previousEntry.toRoute<AppRoute.UnitPreparation>().unitId == route.unitId) {
                            navController.popBackStack()
                        } else navController.navigate(AppRoute.UnitPreparation(route.unitId)) {
                            popUpTo<AppRoute.UnitCall> { inclusive = true }
                            launchSingleTop = true
                        }
                        showCancelledCallNotice()
                    }
                })
            }
            composable<AppRoute.UnitResult> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.UnitResult>()
                val retry: () -> Unit = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(AppRoute.UnitCall(route.unitId, isRetry = true)) {
                            popUpTo<AppRoute.UnitResult> { inclusive = true }
                        }
                    }
                }
                UnitResultRoute(
                    onBack = navController::popBackStack,
                    onContinuePath = returnToPath,
                    onReplayUnit = retry,
                    onRetryUnit = retry,
                    onReviewLesson = {
                        if (navController.currentBackStackEntry == backStackEntry &&
                            !navController.popBackStack(AppRoute.UnitPreparation(route.unitId), false)) {
                            navController.navigate(AppRoute.UnitPreparation(route.unitId)) {
                                popUpTo<AppRoute.UnitResult> { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable<AppRoute.GoalAssessment> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.GoalAssessment>()
                if (route.isCalling) {
                    val preparationEntry = remember(backStackEntry) {
                        navController.previousBackStackEntry?.takeIf {
                            !route.isRetry && it.destination.hasRoute<AppRoute.GoalAssessment>() &&
                                it.toRoute<AppRoute.GoalAssessment>().let { previous -> previous.goalId == route.goalId && !previous.isCalling }
                        }
                    }
                    GoalAssessmentCallRoute(route.isRetry, navController::popBackStack, { attemptId, isReplay ->
                        if (navController.currentBackStackEntry == backStackEntry) {
                            navController.navigate(AppRoute.GoalAssessmentResult(route.goalId, attemptId, isReplay)) {
                                popUpTo<AppRoute.GoalAssessment> { inclusive = true }
                            }
                        }
                    }, viewModel = hiltViewModel(preparationEntry ?: backStackEntry))
                } else {
                    GoalAssessmentRoute(
                        onBack = navController::popBackStack,
                        onStartAssessment = {
                            if (navController.currentBackStackEntry == backStackEntry) {
                                navController.navigate(route.copy(isCalling = true))
                            }
                        },
                        onReviewLessons = { navController.navigate(AppRoute.TrainingPath(route.goalId)) {
                            popUpTo<AppRoute.TrainingPath> { inclusive = true }
                            launchSingleTop = true
                        } }
                    )
                }
            }
            composable<AppRoute.GoalAssessmentResult> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.GoalAssessmentResult>()
                GoalAssessmentResultRoute(
                    onBack = navController::popBackStack,
                    onRetryAssessment = {
                        if (navController.currentBackStackEntry == backStackEntry) {
                            navController.navigate(AppRoute.GoalAssessment(route.goalId, isCalling = true, isRetry = true)) {
                                popUpTo<AppRoute.GoalAssessmentResult> { inclusive = true }
                            }
                        }
                    },
                    onOpenNextGoal = { goalId ->
                        navController.navigate(AppRoute.TrainingPath(goalId)) { popUpTo<AppRoute.TrainingPath> { inclusive = true }; launchSingleTop = true }
                    },
                    onViewCompletedPath = {
                        navController.navigate(AppRoute.PathCompletion) { popUpTo<AppRoute.TrainingPath> { inclusive = true }; launchSingleTop = true }
                    },
                    onReturnToPath = returnToPath,
                    onReviewLessons = {
                        navController.navigate(AppRoute.TrainingPath(route.goalId)) { popUpTo<AppRoute.TrainingPath> { inclusive = true }; launchSingleTop = true }
                    }
                )
            }
            composable<AppRoute.PathCompletion> {
                PathCompletionRoute(
                    onBack = navController::popBackStack,
                    onReturnToPath = returnToPath,
                    onFreePractice = openPractice,
                    onStartPromotion = { navController.navigate(AppRoute.Reassessment) { launchSingleTop = true } }
                )
            }
            composable<AppRoute.ScenarioCatalog> {
                ScenarioCatalogRoute({ navController.navigate(AppRoute.ScenarioPreparation(it)) { launchSingleTop = true } }, openPath)
            }
            composable<AppRoute.ScenarioPreparation> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.ScenarioPreparation>()
                val preparationEntry = remember(backStackEntry) {
                    if (!route.isCalling) null else navController.previousBackStackEntry?.takeIf {
                        it.destination.hasRoute<AppRoute.ScenarioPreparation>() && it.toRoute<AppRoute.ScenarioPreparation>() == route.copy(isCalling = false)
                    }
                }
                ScenarioPreparationRoute(route.isCalling, navController::popBackStack, {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(route.copy(isCalling = true))
                    }
                }, onPracticeCompleted = { attemptId, title ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(AppRoute.PracticeSummary(attemptId, title)) {
                            popUpTo<AppRoute.ScenarioPreparation> { inclusive = true }
                        }
                    }
                }, viewModel = hiltViewModel(preparationEntry ?: backStackEntry), onCallCancelled = {
                    if (route.isCalling && navController.currentBackStackEntry == backStackEntry && authViewModel.isSessionReady(sessionUserId)) {
                        val previousEntry = navController.previousBackStackEntry
                        if (previousEntry?.destination?.hasRoute<AppRoute.ScenarioPreparation>() == true &&
                            previousEntry.toRoute<AppRoute.ScenarioPreparation>() == route.copy(isCalling = false)) {
                            navController.popBackStack()
                        } else navController.navigate(route.copy(isCalling = false)) {
                            popUpTo<AppRoute.ScenarioPreparation> { inclusive = true }
                            launchSingleTop = true
                        }
                        showCancelledCallNotice()
                    }
                }, noticeContent = {
                    if (!route.isCalling && cancellationNoticeEntryId == backStackEntry.id) SnackbarHost(cancellationNoticeHost) { data ->
                        CallExitNoticeCard(data.visuals.message)
                    }
                })
            }
            composable<AppRoute.PracticeSummary> { backStackEntry ->
                PracticeSummaryRoute(onDone = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(AppRoute.ScenarioCatalog) {
                            popUpTo<AppRoute.ScenarioCatalog> { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }, onViewSavedTranscript = { attemptId ->
                    if (navController.currentBackStackEntry == backStackEntry) navController.navigate(AppRoute.SessionDetail(attemptId)) { launchSingleTop = true }
                })
            }
            composable<AppRoute.History> {
                HistoryRoute({ navController.navigate(AppRoute.SessionDetail(it)) { launchSingleTop = true } }, openPractice, openPath)
            }
            composable<AppRoute.SessionDetail> { SessionDetailRoute(navController::popBackStack) }
            composable<AppRoute.Profile> {
                ProfileRoute(
                    onOpenSettings = { navController.navigate(AppRoute.Settings) { launchSingleTop = true } },
                    onOpenVocabulary = { navController.navigate(AppRoute.SavedVocabulary) { launchSingleTop = true } },
                    onStartPlacement = openPlacement,
                    onSignedOut = { navController.navigate(AppRoute.Auth) { popUpTo(navController.graph.id) { inclusive = true } } }
                )
            }
            composable<AppRoute.Settings> { SettingsRoute(navController::popBackStack) }
            composable<AppRoute.SavedVocabulary> { SavedVocabRoute(onNavigateBack = navController::popBackStack, onStartPractice = openPractice) }
        }
        }
    }
}

private fun navigateTab(navController: NavHostController, route: AppRoute) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
