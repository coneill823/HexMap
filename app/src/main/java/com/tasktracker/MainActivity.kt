package com.tasktracker

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val application = applicationContext as TaskTrackerApplication

        setContent {
            val theme by application.themeRepository.currentTheme
                .collectAsState(initial = AppTheme.PURPLE)
            TaskTrackerTheme(appTheme = theme) {
                AppNavigation(application = application)
            }
        }
    }
}
