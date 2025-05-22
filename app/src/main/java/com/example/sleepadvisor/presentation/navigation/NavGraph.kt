package com.example.sleepadvisor.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sleepadvisor.presentation.screens.analysis.SleepAnalysisScreen
import com.example.sleepadvisor.presentation.screens.home.HomeScreen
import com.example.sleepadvisor.presentation.screens.sleep.AddManualSleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.EditManualSleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import com.example.sleepadvisor.presentation.viewmodel.SleepAnalysisViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Sleep : Screen("sleep")
    object AddManualSleep : Screen("add_manual_sleep")
    object EditManualSleep : Screen("edit_manual_sleep/{sessionId}") {
        fun createRoute(sessionId: String) = "edit_manual_sleep/$sessionId"
    }
    object SleepAnalysis : Screen("sleep_analysis")
    object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: String) = "session_detail/$sessionId"
    }
}

@Composable
fun AppNavigation(
    viewModel: SleepViewModel,
    sleepAnalysisViewModel: SleepAnalysisViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAnalysis = {
                    navController.navigate(Screen.SleepAnalysis.route)
                },
                onNavigateToManualEntry = { sessionId ->
                    if (sessionId == null) {
                        navController.navigate(Screen.AddManualSleep.route)
                    } else {
                        navController.navigate(Screen.EditManualSleep.createRoute(sessionId))
                    }
                }
            )
        }
        composable(Screen.Sleep.route) {
            SleepScreen(
                viewModel = viewModel,
                onNavigateToManualEntry = { sessionId ->
                    if (sessionId == null) {
                        navController.navigate(Screen.AddManualSleep.route)
                    } else {
                        navController.navigate(Screen.EditManualSleep.createRoute(sessionId))
                    }
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.SleepAnalysis.route)
                }
            )
        }
        
        composable(Screen.AddManualSleep.route) {
            AddManualSleepScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditManualSleep.route
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
                ?: throw IllegalStateException("sessionId parameter not found")
            
            EditManualSleepScreen(
                sessionId = sessionId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SleepAnalysis.route) {
            SleepAnalysisScreen(
                viewModel = sleepAnalysisViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                }
            )
        }
        
        composable(
            route = Screen.SessionDetail.route
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
                ?: throw IllegalStateException("sessionId parameter not found")
            
            // Implementar tela de detalhes da sessão
            // SessionDetailScreen(
            //     sessionId = sessionId,
            //     viewModel = sleepAnalysisViewModel,
            //     onNavigateBack = {
            //         navController.popBackStack()
            //     }
            // )
        }
    }
} 