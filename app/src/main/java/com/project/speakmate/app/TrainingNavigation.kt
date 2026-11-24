package com.project.speakmate.app

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.project.speakmate.ui.training.GoalAssessmentCallRoute
import com.project.speakmate.ui.training.GoalAssessmentResultRoute
import com.project.speakmate.ui.training.GoalAssessmentRoute
import com.project.speakmate.ui.training.PathCompletionRoute
import com.project.speakmate.ui.training.TrainingPathRoute
import com.project.speakmate.ui.training.UnitCallRoute
import com.project.speakmate.ui.training.UnitPreparationRoute
import com.project.speakmate.ui.training.UnitResultRoute

fun NavGraphBuilder.trainingNavigation(
    actions: AppNavigationActions,
    navController: NavHostController,
    notice: AppNavigationNotice
) {
    composable<AppRoute.TrainingPath> { pathBackStackEntry ->
        TrainingPathRoute(
            goalId = pathBackStackEntry.toRoute<AppRoute.TrainingPath>().goalId,
            onOpenActivity = { activity ->
                if (navController.currentBackStackEntry == pathBackStackEntry)
                    actions.openActivity(activity)
            },
            onStartPlacement = actions::openPlacement,
            onFreePractice = actions::openPractice,
            onViewCompletedPath = {
                navController.navigate(AppRoute.PathCompletion) { launchSingleTop = true }
            }
        )
    }
    composable<AppRoute.UnitPreparation> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.UnitPreparation>()
        UnitPreparationRoute(
            unitId = route.unitId,
            onBack = navController::popBackStack,
            onStartRoleplay = {
                navController.navigate(AppRoute.UnitCall(route.unitId)) { launchSingleTop = true }
            },
            noticeContent = {
                if (notice.entryId == backStackEntry.id)
                    AppNavigationNoticeContent(hostState = notice.hostState)
            }
        )
    }
    composable<AppRoute.UnitCall> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.UnitCall>()
        val preparationEntry =
            remember(backStackEntry) {
                navController.previousBackStackEntry?.takeIf {
                    !route.isRetry &&
                        it.destination.hasRoute<AppRoute.UnitPreparation>() &&
                        it.toRoute<AppRoute.UnitPreparation>().unitId == route.unitId
                }
            }
        UnitCallRoute(
            unitId = route.unitId,
            isRetry = route.isRetry,
            onBack = navController::popBackStack,
            onCompleted = { attemptId, isReplay ->
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate(
                        AppRoute.UnitResult(
                            unitId = route.unitId,
                            attemptId = attemptId,
                            isReplay = isReplay
                        )
                    ) {
                        popUpTo<AppRoute.UnitCall> { inclusive = true }
                    }
                }
            },
            viewModel = hiltViewModel(preparationEntry ?: backStackEntry),
            onCallCancelled = {
                if (navController.currentBackStackEntry == backStackEntry &&
                    actions.isSessionReady) {
                    val previousEntry = navController.previousBackStackEntry
                    actions.returnToPreparation(
                        destination = AppRoute.UnitPreparation(route.unitId),
                        shouldPop = previousEntry?.destination?.hasRoute<AppRoute.UnitPreparation>() == true &&
                            previousEntry.toRoute<AppRoute.UnitPreparation>().unitId == route.unitId,
                        popUpToRoute = AppRoute.UnitCall::class
                    )
                }
            }
        )
    }
    composable<AppRoute.UnitResult> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.UnitResult>()
        val retry: () -> Unit = {
            if (navController.currentBackStackEntry == backStackEntry) {
                navController.navigate(AppRoute.UnitCall(unitId = route.unitId, isRetry = true)) {
                    popUpTo<AppRoute.UnitResult> { inclusive = true }
                }
            }
        }
        UnitResultRoute(
            onBack = navController::popBackStack,
            onContinuePath = actions::returnToPath,
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
            val preparationEntry =
                remember(backStackEntry) {
                    navController.previousBackStackEntry?.takeIf {
                        !route.isRetry &&
                            it.destination.hasRoute<AppRoute.GoalAssessment>() &&
                            it.toRoute<AppRoute.GoalAssessment>().let { previous ->
                                previous.goalId == route.goalId && !previous.isCalling
                            }
                    }
                }
            GoalAssessmentCallRoute(
                isRetry = route.isRetry,
                onBack = navController::popBackStack,
                onCompleted = { attemptId, isReplay ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(
                            AppRoute.GoalAssessmentResult(
                                goalId = route.goalId,
                                attemptId = attemptId,
                                isReplay = isReplay
                            )
                        ) {
                            popUpTo<AppRoute.GoalAssessment> { inclusive = true }
                        }
                    }
                },
                viewModel = hiltViewModel(preparationEntry ?: backStackEntry)
            )
        } else {
            GoalAssessmentRoute(
                onBack = navController::popBackStack,
                onStartAssessment = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(route.copy(isCalling = true))
                    }
                },
                onReviewLessons = { actions.openGoal(route.goalId) }
            )
        }
    }
    composable<AppRoute.GoalAssessmentResult> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.GoalAssessmentResult>()
        GoalAssessmentResultRoute(
            onBack = navController::popBackStack,
            onRetryAssessment = {
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate(
                        AppRoute.GoalAssessment(
                            goalId = route.goalId,
                            isCalling = true,
                            isRetry = true
                        )
                    ) {
                        popUpTo<AppRoute.GoalAssessmentResult> { inclusive = true }
                    }
                }
            },
            onOpenNextGoal = actions::openGoal,
            onViewCompletedPath = {
                navController.navigate(AppRoute.PathCompletion) {
                    popUpTo<AppRoute.TrainingPath> { inclusive = false }
                    launchSingleTop = true
                }
            },
            onReturnToPath = actions::returnToPath,
            onReviewLessons = { actions.openGoal(route.goalId) }
        )
    }
    composable<AppRoute.PathCompletion> {
        PathCompletionRoute(
            onBack = navController::popBackStack,
            onReturnToPath = actions::returnToPath,
            onFreePractice = actions::openPractice,
            onStartPromotion = {
                navController.navigate(AppRoute.Reassessment) { launchSingleTop = true }
            }
        )
    }
}
