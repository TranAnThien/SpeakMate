package com.project.speakmate.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.speakmate.ui.auth.AuthViewModel
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    startDestination: AppRoute,
    sessionUserId: String,
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val notice = rememberAppCallNotice(navController = navController, sessionUserId = sessionUserId, entry = entry)
    val actions = rememberAppNavigationActions(
        navController = navController,
        authViewModel = authViewModel,
        sessionUserId = sessionUserId,
        showCancelledCallNotice = notice.showCancelledCall
    )
    val destination = entry?.destination
    val selectedTab =
        when {
            destination?.hasRoute<AppRoute.TrainingPath>() == true -> AppDestination.TRAINING
            destination?.hasRoute<AppRoute.ScenarioCatalog>() == true -> AppDestination.PRACTICE
            destination?.hasRoute<AppRoute.History>() == true -> AppDestination.HISTORY
            destination?.hasRoute<AppRoute.Profile>() == true -> AppDestination.PROFILE
            else -> null
        }
    Scaffold(
        containerColor = DesignColor.Background,
        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0),
        bottomBar = {
            if (selectedTab != null) {
                AppBottomBar(
                    onSelectDestination = actions::selectTab,
                    selectedDestination = selectedTab,
                    bottomPadding = if (selectedTab == AppDestination.TRAINING) 12.dp else 4.dp
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f)
            ) {
                placementNavigation(actions = actions, navController = navController)
                trainingNavigation(actions = actions, navController = navController, notice = notice)
                practiceNavigation(actions = actions, navController = navController, notice = notice)
                profileNavigation(actions = actions, navController = navController)
            }
        }
    }
}
