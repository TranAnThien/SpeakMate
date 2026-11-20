package com.project.speakmate.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.navigation
import com.project.speakmate.ui.auth.AuthRoute
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
import com.project.speakmate.ui.history.HistoryRoute
import com.project.speakmate.ui.history.SessionDetailRoute
import com.project.speakmate.ui.home.HomeRoute
import com.project.speakmate.ui.placement.PlacementRoute
import com.project.speakmate.ui.placement.ReassessmentRoute
import com.project.speakmate.ui.practice.ScenarioCatalogRoute
import com.project.speakmate.ui.practice.ScenarioPreparationRoute
import com.project.speakmate.ui.profile.ProfileRoute
import com.project.speakmate.ui.profile.SettingsRoute
import com.project.speakmate.ui.profile.savedvocab.SavedVocabRoute
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.training.GoalAssessmentCallRoute
import com.project.speakmate.ui.training.GoalAssessmentResultRoute
import com.project.speakmate.ui.training.GoalAssessmentRoute
import com.project.speakmate.ui.training.GoalDetailRoute
import com.project.speakmate.ui.training.PathCompletionRoute
import com.project.speakmate.ui.training.TrainingPathRoute
import com.project.speakmate.ui.training.UnitCallRoute
import com.project.speakmate.ui.training.UnitPreparationRoute
import com.project.speakmate.ui.training.UnitResultRoute
import com.project.speakmate.ui.training.state.TrainingActivityDestination

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    startDestination: AppRoute,
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val selectedTab = when {
        destination?.hasRoute<AppRoute.Home>() == true || destination?.hasRoute<AppRoute.TrainingPath>() == true -> AppDestination.TRAINING
        destination?.hasRoute<AppRoute.ScenarioCatalog>() == true -> AppDestination.PRACTICE
        destination?.hasRoute<AppRoute.History>() == true -> AppDestination.HISTORY
        destination?.hasRoute<AppRoute.Profile>() == true -> AppDestination.PROFILE
        else -> null
    }
    val openPath = remember(navController) { { navigateTab(navController, AppRoute.TrainingPath) } }
    val openPractice = remember(navController) { { navigateTab(navController, AppRoute.ScenarioCatalog) } }
    val openProfile = remember(navController) { { navigateTab(navController, AppRoute.Profile) } }
    val returnToPath = remember(navController) {
        {
            if (!navController.popBackStack(AppRoute.TrainingPath, false)) {
                navController.navigate(AppRoute.TrainingPath) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
            Unit
        }
    }
    val returnHome = remember(navController) {
        {
            navController.navigate(AppRoute.Home) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }
    val openActivity: (TrainingActivityDestination) -> Unit = remember(navController) {
        { activity ->
            when (activity) {
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
                            AppDestination.TRAINING -> AppRoute.TrainingPath
                            AppDestination.PRACTICE -> AppRoute.ScenarioCatalog
                            AppDestination.HISTORY -> AppRoute.History
                            AppDestination.PROFILE -> AppRoute.Profile
                        }
                        navigateTab(navController, route)
                    },
                    selectedDestination = selectedTab,
                    modifier = Modifier.navigationBarsPadding(),
                    bottomPadding = if (selectedTab == AppDestination.TRAINING) 12.dp else 4.dp
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination, Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable<AppRoute.Auth> {
                AuthRoute(
                    onSignedIn = {
                        if (navController.currentDestination?.hasRoute<AppRoute.Auth>() == true) {
                            navController.navigate(AppRoute.Home) { popUpTo<AppRoute.Auth> { inclusive = true } }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable<AppRoute.Home> {
                HomeRoute(
                    onStartAssessment = { navController.navigate(AppRoute.Placement) { launchSingleTop = true } },
                    onContinuePath = openPath,
                    onStartReassessment = { navController.navigate(AppRoute.Reassessment) { launchSingleTop = true } },
                    onSelectScenario = openPractice,
                    onViewProfile = openProfile
                )
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
                            onHome = returnHome
                        )
                    }
                }
            }
            composable<AppRoute.TrainingPath> {
                TrainingPathRoute(
                    onOpenGoal = { navController.navigate(AppRoute.GoalDetail(it)) { launchSingleTop = true } },
                    onOpenActivity = openActivity
                )
            }
            composable<AppRoute.GoalDetail> { backStackEntry ->
                GoalDetailRoute(backStackEntry.toRoute<AppRoute.GoalDetail>().goalId, navController::popBackStack, openPath, openActivity)
            }
            composable<AppRoute.UnitPreparation> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.UnitPreparation>()
                UnitPreparationRoute(route.unitId, navController::popBackStack, {
                    navController.navigate(AppRoute.UnitCall(route.unitId)) { launchSingleTop = true }
                })
            }
            composable<AppRoute.UnitCall> { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.UnitCall>()
                UnitCallRoute(route.unitId, route.isRetry, navController::popBackStack, { attemptId ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(AppRoute.UnitResult(route.unitId, attemptId)) {
                            popUpTo<AppRoute.UnitCall> { inclusive = true }
                        }
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
                    GoalAssessmentCallRoute(route.isRetry, navController::popBackStack, { attemptId, isReplay ->
                        if (navController.currentBackStackEntry == backStackEntry) {
                            navController.navigate(AppRoute.GoalAssessmentResult(route.goalId, attemptId, isReplay)) {
                                popUpTo<AppRoute.GoalAssessment> { inclusive = true }
                            }
                        }
                    })
                } else {
                    GoalAssessmentRoute(
                        onBack = navController::popBackStack,
                        onStartAssessment = {
                            if (navController.currentBackStackEntry == backStackEntry) {
                                navController.navigate(route.copy(isCalling = true))
                            }
                        },
                        onReviewLessons = { navController.navigate(AppRoute.GoalDetail(route.goalId)) }
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
                        navController.navigate(AppRoute.GoalDetail(goalId)) { popUpTo<AppRoute.GoalAssessment> { inclusive = true } }
                    },
                    onViewCompletedPath = {
                        navController.navigate(AppRoute.PathCompletion) { popUpTo<AppRoute.GoalAssessment> { inclusive = true } }
                    },
                    onReturnToPath = returnToPath,
                    onReviewLessons = {
                        navController.navigate(AppRoute.GoalDetail(route.goalId)) { popUpTo<AppRoute.GoalAssessment> { inclusive = true } }
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
                ScenarioPreparationRoute(route.isCalling, navController::popBackStack, {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(route.copy(isCalling = true))
                    }
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
                    onSignedOut = { navController.navigate(AppRoute.Auth) { popUpTo(navController.graph.id) { inclusive = true } } }
                )
            }
            composable<AppRoute.Settings> { SettingsRoute(navController::popBackStack) }
            composable<AppRoute.SavedVocabulary> { SavedVocabRoute(onNavigateBack = navController::popBackStack) }
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
