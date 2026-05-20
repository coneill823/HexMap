package com.tasktracker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tasktracker.MainActivity
import com.tasktracker.TaskTrackerApplication
import com.tasktracker.data.database.entities.Routine
import kotlinx.coroutines.flow.first

class RoutineStartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TaskTrackerApplication
        val routines: List<Routine> = try {
            app.routineRepository.getAllRoutinesWithItems().first()
                .map { it.routine }
                .take(4)
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1A1A2E)))
                        .padding(12.dp)
                ) {
                    Text(
                        "My Routines",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9C71FF)),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))

                    if (routines.isEmpty()) {
                        Text(
                            "No routines yet.\nCreate one in the app.",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFC4C0DC))
                            )
                        )
                    } else {
                        routines.forEach { routine ->
                            val launchIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("start_routine_id", routine.id)
                            }
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(actionStartActivity(launchIntent)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "▶ ${routine.name}",
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFE4E1F5))
                                    ),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                routine.timeMinutes?.let { mins ->
                                    Text(
                                        "${mins / 60}:${(mins % 60).toString().padStart(2, '0')}",
                                        style = TextStyle(
                                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9C71FF))
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class RoutineStartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RoutineStartWidget()
}
