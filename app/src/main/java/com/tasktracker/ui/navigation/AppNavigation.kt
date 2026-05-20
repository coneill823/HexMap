package com.tasktracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
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
import com.tasktracker.ui.screens.graphs.GraphsScreen
import com.tasktracker.ui.screens.graphs.GraphsViewModel
import com.tasktracker.ui.screens.options.OptionsScreen
import com.tasktracker.ui.screens.options.OptionsViewModel
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
    object Graphs : Screen("graphs", "Graphs", Icons.Filled.ShowChart, Icons.Filled.ShowChart)
    object Options : Screen("options", "Options", Icons.Filled.Settings, Icons.Filled.Settings)
}

val bottomNavScreens = listOf(Screen.Calendar, Screen.Tasks, Screen.YearView, Screen.Graphs, Screen.Options)

@Composable
fun AppNavigation(
    application: TaskTrackerApplication,
    widgetStartRoutineId: Long? = null,
    widgetStartTaskId: Long? = null,
    onWidgetStartHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    // Shared state: when Calendar requests editing a routine, navigate to Tasks tab
    var navigateToRoutineId by remember { mutableStateOf<Long?>(null) }

    val calendarVm: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(
            application,
            application.taskRepository,
            application.routineRepository,
            application.recurrenceRepository,
            application.sessionLogRepository
        )
    )
    val tasksVm: TasksViewModel = viewModel(
        factory = TasksViewModel.Factory(
            application,
            application.taskRepository,
            application.routineRepository,
            application.sessionLogRepository,
            application.recurrenceRepository
        )
    )

    LaunchedEffect(widgetStartRoutineId) {
        if (widgetStartRoutineId != null) {
            tasksVm.startRoutineById(widgetStartRoutineId)
            navController.navigate(Screen.Tasks.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onWidgetStartHandled()
        }
    }

    LaunchedEffect(widgetStartTaskId) {
        if (widgetStartTaskId != null) {
            tasksVm.startTaskPlayById(widgetStartTaskId)
            navController.navigate(Screen.Tasks.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onWidgetStartHandled()
        }
    }
    val yearVm: YearViewViewModel = viewModel(
        factory = YearViewViewModel.Factory(
            application.routineRepository,
            application.taskRepository
        )
    )
    val graphsVm: GraphsViewModel = viewModel(
        factory = GraphsViewModel.Factory(application.routineRepository, application.sessionLogRepository)
    )
    val optionsVm: OptionsViewModel = viewModel(
        factory = OptionsViewModel.Factory(application.themeRepository, application.routineRepository)
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
                CalendarScreen(
                    viewModel = calendarVm,
                    onNavigateToRoutine = { routineId ->
                        navigateToRoutineId = routineId
                        tasksVm.selectRoutineToEdit(routineId)
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(viewModel = tasksVm)
            }
            composable(Screen.YearView.route) {
                YearViewScreen(viewModel = yearVm)
            }
            composable(Screen.Graphs.route) {
                GraphsScreen(viewModel = graphsVm)
            }
            composable(Screen.Options.route) {
                OptionsScreen(viewModel = optionsVm)
            }
        }
    }
}
