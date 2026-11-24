package com.haikal.xboxcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import com.haikal.xboxcontroller.navigation.Screen
import com.haikal.xboxcontroller.ui.console.ConsoleSelectionScreen
import com.haikal.xboxcontroller.ui.controller.VirtualControllerScreen
import com.haikal.xboxcontroller.ui.home.HomeScreen
import com.haikal.xboxcontroller.ui.settings.SettingsScreen
import com.haikal.xboxcontroller.ui.theme.XboxVirtualControllerTheme
import com.haikal.xboxcontroller.viewmodel.ControllerViewModel
import com.haikal.xboxcontroller.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XboxVirtualControllerTheme {
                val homeViewModel = HomeViewModel(application)
                val controllerViewModel = ControllerViewModel(application)
                XboxApp(homeViewModel, controllerViewModel)
            }
        }
    }
}

@Composable
fun XboxApp(homeViewModel: HomeViewModel, controllerViewModel: ControllerViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOpenConsoleSelection = {
                            navController.navigate(Screen.ConsoleSelection.route)
                        },
                        onOpenController = {
                            navController.navigate(Screen.VirtualController.route)
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                    )
                }
                composable(Screen.ConsoleSelection.route) {
                    ConsoleSelectionScreen(
                        viewModel = homeViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.VirtualController.route) {
                    VirtualControllerScreen(
                        viewModel = controllerViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = homeViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
