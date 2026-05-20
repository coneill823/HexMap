package com.tasktracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.tasktracker.data.models.AppTheme
import com.tasktracker.ui.navigation.AppNavigation
import com.tasktracker.ui.theme.TaskTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val _widgetStartRoutineId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        intent?.getLongExtra("start_routine_id", -1L)?.takeIf { it != -1L }?.let {
            _widgetStartRoutineId.value = it
        }

        val application = applicationContext as TaskTrackerApplication

        setContent {
            val theme by application.themeRepository.currentTheme
                .collectAsState(initial = AppTheme.PURPLE)
            val widgetRoutineId by _widgetStartRoutineId.collectAsState()
            TaskTrackerTheme(appTheme = theme) {
                AppNavigation(
                    application = application,
                    widgetStartRoutineId = widgetRoutineId,
                    onWidgetStartHandled = { _widgetStartRoutineId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getLongExtra("start_routine_id", -1L).takeIf { it != -1L }?.let {
            _widgetStartRoutineId.value = it
        }
    }
}
