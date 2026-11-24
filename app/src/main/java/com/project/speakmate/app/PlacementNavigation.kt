package com.project.speakmate.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.project.speakmate.ui.placement.PlacementRoute
import com.project.speakmate.ui.placement.ReassessmentRoute

fun NavGraphBuilder.placementNavigation(actions: AppNavigationActions, navController: NavHostController) {
    navigation<AppRoute.Placement>(startDestination = AppRoute.PlacementStep()) {
        composable<AppRoute.PlacementStep> { backStackEntry ->
            val graphEntry =
                remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<AppRoute.Placement>()
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            val graphLifecycleState = graphEntry?.lifecycle?.currentStateAsState()?.value
            if (graphEntry == null || graphLifecycleState == Lifecycle.State.DESTROYED) {
                LaunchedEffect(backStackEntry) {
                    if (navController.currentBackStackEntry == backStackEntry) actions.returnToPath()
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
                    onHome = actions::returnToPath
                )
            }
        }
    }
    navigation<AppRoute.Reassessment>(startDestination = AppRoute.ReassessmentStep()) {
        composable<AppRoute.ReassessmentStep> { backStackEntry ->
            val graphEntry =
                remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry<AppRoute.Reassessment>()
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            val graphLifecycleState = graphEntry?.lifecycle?.currentStateAsState()?.value
            if (graphEntry == null || graphLifecycleState == Lifecycle.State.DESTROYED) {
                LaunchedEffect(backStackEntry) {
                    if (navController.currentBackStackEntry == backStackEntry) actions.returnToPath()
                }
            } else {
                ReassessmentRoute(
                    viewModel = hiltViewModel(graphEntry),
                    isCallDestination =
                        backStackEntry.toRoute<AppRoute.ReassessmentStep>().isCalling,
                    onChangeCallDestination = { isCalling ->
                        if (navController.currentBackStackEntry == backStackEntry) {
                            navController.navigate(AppRoute.ReassessmentStep(isCalling)) {
                                popUpTo<AppRoute.ReassessmentStep> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onReturnToPath = actions::returnToPath,
                    onOpenPromotedPath = actions::returnToPath,
                    onHome = actions::returnToPath,
                    onOpenProfile = {
                        if (navController.currentBackStackEntry == backStackEntry)
                            navController.navigate(AppRoute.Profile) {
                                popUpTo<AppRoute.Reassessment> { inclusive = true }
                                launchSingleTop = true
                            }
                    }
                )
            }
        }
    }
}
