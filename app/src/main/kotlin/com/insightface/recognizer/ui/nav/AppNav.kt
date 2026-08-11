package com.insightface.recognizer.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.insightface.recognizer.ui.home.HomeScreen
import com.insightface.recognizer.ui.manage.ManageScreen
import com.insightface.recognizer.ui.recognize.RecognizeScreen
import com.insightface.recognizer.ui.settings.SettingsScreen

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "首页", Icons.Outlined.Home)
    data object Recognize : Dest("recognize", "识别", Icons.Outlined.ImageSearch)
    data object Manage : Dest("manage", "人脸库", Icons.Outlined.VerifiedUser)
    data object Settings : Dest("settings", "设置", Icons.Outlined.Settings)
}

private val destinations = listOf(Dest.Home, Dest.Recognize, Dest.Manage, Dest.Settings)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    // Shared navigation logic used by both the bottom bar and HomeScreen action cards.
    // Uses popUpTo(startDestination){saveState=true} + launchSingleTop + restoreState so
    // tab switching is consistent regardless of which entry point triggered the navigation.
    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateTo(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Home.route) { HomeScreen(navigateTo) }
            composable(Dest.Recognize.route) { RecognizeScreen() }
            composable(Dest.Manage.route) { ManageScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
