package com.haikal.xboxcontroller.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Type-safe-ish route definitions for the NavHost.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object ConsoleSelection : Screen("console_selection", "Console Selection")
    data object VirtualController : Screen("virtual_controller", "Virtual Controller", Icons.Filled.SportsEsports)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val all = listOf(Home, ConsoleSelection, VirtualController, Settings)
    }
}
