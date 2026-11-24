package com.project.speakmate.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.training.state.TrainingActivityDestination
import kotlin.reflect.KClass

class AppNavigationActions(
    private val navController: NavHostController,
    private val authViewModel: AuthViewModel,
    private val sessionUserId: String,
    private val showCancelledCallNotice: () -> Unit
) {
    val isSessionReady: Boolean
        get() = authViewModel.isSessionReady(sessionUserId)

    fun openPath() {
        navigateTab(AppRoute.TrainingPath())
    }

    fun openPractice() {
        navigateTab(AppRoute.ScenarioCatalog)
    }

    fun returnToPath() {
        if (!navController.popBackStack<AppRoute.TrainingPath>(inclusive = false)) {
            navController.navigate(AppRoute.TrainingPath()) {
                popUpTo(navController.graph.id)
                launchSingleTop = true
            }
        }
    }

    fun openPlacement() {
        if (authViewModel.isSessionReady(sessionUserId))
            navController.navigate(AppRoute.Placement) { launchSingleTop = true }
    }

    fun openActivity(activity: TrainingActivityDestination) {
        if (authViewModel.isSessionReady(sessionUserId))
            when (activity) {
                is TrainingActivityDestination.Unit ->
                    navController.navigate(AppRoute.UnitPreparation(activity.unitId)) { launchSingleTop = true }
                is TrainingActivityDestination.GoalAssessment ->
                    navController.navigate(AppRoute.GoalAssessment(activity.goalId)) { launchSingleTop = true }
            }
    }

    fun selectTab(tab: AppDestination) {
        val route =
            when (tab) {
                AppDestination.TRAINING -> AppRoute.TrainingPath()
                AppDestination.PRACTICE -> AppRoute.ScenarioCatalog
                AppDestination.HISTORY -> AppRoute.History
                AppDestination.PROFILE -> AppRoute.Profile
            }
        navigateTab(route)
    }

    fun openGoal(goalId: String) {
        navController.navigate(AppRoute.TrainingPath(goalId)) {
            popUpTo<AppRoute.TrainingPath> { inclusive = true }
            launchSingleTop = true
        }
    }

    fun returnToPreparation(destination: AppRoute, shouldPop: Boolean, popUpToRoute: KClass<out AppRoute>) {
        if (shouldPop) {
            navController.popBackStack()
        } else
            navController.navigate(destination) {
                popUpTo(popUpToRoute) { inclusive = true }
                launchSingleTop = true
            }
        showCancelledCallNotice()
    }

    private fun navigateTab(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberAppNavigationActions(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    sessionUserId: String,
    showCancelledCallNotice: () -> Unit
): AppNavigationActions =
    remember(navController, authViewModel, sessionUserId, showCancelledCallNotice) {
        AppNavigationActions(
            navController = navController,
            authViewModel = authViewModel,
            sessionUserId = sessionUserId,
            showCancelledCallNotice = showCancelledCallNotice
        )
    }
