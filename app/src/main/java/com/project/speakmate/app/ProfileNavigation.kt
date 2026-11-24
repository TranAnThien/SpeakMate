package com.project.speakmate.app

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.project.speakmate.ui.history.HistoryRoute
import com.project.speakmate.ui.history.SessionDetailRoute
import com.project.speakmate.ui.profile.ProfileRoute
import com.project.speakmate.ui.profile.SettingsRoute
import com.project.speakmate.ui.profile.savedvocab.SavedVocabRoute

fun NavGraphBuilder.profileNavigation(actions: AppNavigationActions, navController: NavHostController) {
    composable<AppRoute.History> {
        HistoryRoute(
            onOpenSession = {
                navController.navigate(AppRoute.SessionDetail(it)) { launchSingleTop = true }
            },
            onOpenPractice = actions::openPractice,
            onOpenTrainingPath = actions::openPath
        )
    }
    composable<AppRoute.SessionDetail> { SessionDetailRoute(navController::popBackStack) }
    composable<AppRoute.Profile> {
        ProfileRoute(
            onOpenSettings = {
                navController.navigate(AppRoute.Settings) { launchSingleTop = true }
            },
            onOpenVocabulary = {
                navController.navigate(AppRoute.SavedVocabulary) { launchSingleTop = true }
            },
            onStartPlacement = actions::openPlacement
        )
    }
    composable<AppRoute.Settings> { SettingsRoute(navController::popBackStack) }
    composable<AppRoute.SavedVocabulary> {
        SavedVocabRoute(
            onNavigateBack = navController::popBackStack,
            onStartPractice = actions::openPractice
        )
    }
}
