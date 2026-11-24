package com.project.speakmate.app

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.project.speakmate.ui.practice.PracticeSummaryRoute
import com.project.speakmate.ui.practice.ScenarioCatalogRoute
import com.project.speakmate.ui.practice.ScenarioPreparationRoute

fun NavGraphBuilder.practiceNavigation(
    actions: AppNavigationActions,
    navController: NavHostController,
    notice: AppNavigationNotice
) {
    composable<AppRoute.ScenarioCatalog> {
        ScenarioCatalogRoute(
            { navController.navigate(AppRoute.ScenarioPreparation(it)) { launchSingleTop = true } },
            actions::openPath
        )
    }
    composable<AppRoute.ScenarioPreparation> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.ScenarioPreparation>()
        val preparationEntry =
            remember(backStackEntry) {
                if (!route.isCalling) null
                else
                    navController.previousBackStackEntry?.takeIf {
                        it.destination.hasRoute<AppRoute.ScenarioPreparation>() &&
                            it.toRoute<AppRoute.ScenarioPreparation>() ==
                                route.copy(isCalling = false)
                    }
            }
        ScenarioPreparationRoute(
            isCalling = route.isCalling,
            onBack = navController::popBackStack,
            onStartCall = {
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate(route.copy(isCalling = true))
                }
            },
            onPracticeCompleted = { attemptId, title ->
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate(AppRoute.PracticeSummary(attemptId, title)) {
                        popUpTo<AppRoute.ScenarioPreparation> { inclusive = true }
                    }
                }
            },
            viewModel = hiltViewModel(preparationEntry ?: backStackEntry),
            onCallCancelled = {
                if (route.isCalling &&
                    navController.currentBackStackEntry == backStackEntry &&
                    actions.isSessionReady) {
                    val previousEntry = navController.previousBackStackEntry
                    actions.returnToPreparation(
                        destination = route.copy(isCalling = false),
                        shouldPop = previousEntry?.destination?.hasRoute<AppRoute.ScenarioPreparation>() == true &&
                            previousEntry.toRoute<AppRoute.ScenarioPreparation>() == route.copy(isCalling = false),
                        popUpToRoute = AppRoute.ScenarioPreparation::class
                    )
                }
            },
            noticeContent = {
                if (!route.isCalling && notice.entryId == backStackEntry.id)
                    AppNavigationNoticeContent(hostState = notice.hostState)
            }
        )
    }
    composable<AppRoute.PracticeSummary> { backStackEntry ->
        PracticeSummaryRoute(
            onDone = {
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate(AppRoute.ScenarioCatalog) {
                        popUpTo<AppRoute.ScenarioCatalog> { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            onViewSavedTranscript = { attemptId ->
                if (navController.currentBackStackEntry == backStackEntry)
                    navController.navigate(AppRoute.SessionDetail(attemptId)) {
                        launchSingleTop = true
                    }
            }
        )
    }
}
