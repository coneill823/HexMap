package com.tasktracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tasktracker.TaskTrackerApplication
import com.tasktracker.ui.screens.calendar.CalendarScreen
import com.tasktracker.ui.screens.calendar.CalendarViewModel
import com.tasktracker.ui.screens.tasks.TasksScreen
import com.tasktracker.ui.screens.tasks.TasksViewModel
import com.tasktracker.ui.screens.yearview.YearViewScreen
import com.tasktracker.ui.screens.yearview.YearViewViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Calendar : Screen("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Tasks : Screen("tasks", "Tasks & Routines", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline)
    object YearView : Screen("year_view", "Overview", Icons.Filled.GridView, Icons.Outlined.GridView)
}

val bottomNavScreens = listOf(Screen.Calendar, Screen.Tasks, Screen.YearView)

@Composable
fun AppNavigation(application: TaskTrackerApplication) {
    val navController = rememberNavController()

    val calendarVm: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(
            application.taskRepository,
            application.routineRepository,
            application.recurrenceRepository
        )
    )
    val tasksVm: TasksViewModel = viewModel(
        factory = TasksViewModel.Factory(
            application.taskRepository,
            application.routineRepository,
            application.sessionLogRepository
        )
    )
    val yearVm: YearViewViewModel = viewModel(
        factory = YearViewViewModel.Factory(application.routineRepository)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavScreens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = calendarVm)
            }
            composable(Screen.Tasks.route) {
                TasksScreen(viewModel = tasksVm)
            }
            composable(Screen.YearView.route) {
                YearViewScreen(viewModel = yearVm)
            }
        }
    }
}
