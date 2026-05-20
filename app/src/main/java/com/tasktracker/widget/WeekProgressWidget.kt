package com.tasktracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tasktracker.MainActivity
import com.tasktracker.TaskTrackerApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class WeekProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TaskTrackerApplication
        val routineRepo = app.routineRepository

        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekStart = today.with(weekFields.dayOfWeek(), 1)

        data class RoutineProgress(val name: String, val completed: Int, val total: Int)

        val progressList = mutableListOf<RoutineProgress>()
        try {
            val routinesWithItems = routineRepo.getAllRoutinesWithItems().first()
            routinesWithItems.take(3).forEach { rwi ->
                val total = rwi.items.size
                if (total > 0) {
                    var weekCompleted = 0
                    for (i in 0..6) {
                        val day = weekStart.plusDays(i.toLong())
                        if (!day.isAfter(today)) {
                            val completions = routineRepo.getCompletionsForDate(day.toEpochDay()).first()
                            val dayCompleted = completions.count {
                                it.routineId == rwi.routine.id && it.isCompleted
                            }
                            weekCompleted += dayCompleted
                        }
                    }
                    val daysElapsed = (today.dayOfWeek.value).coerceAtLeast(1)
                    val expectedTotal = total * daysElapsed
                    progressList.add(RoutineProgress(rwi.routine.name, weekCompleted, expectedTotal))
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val overallProgress = if (progressList.isNotEmpty()) {
            val totalCompleted = progressList.sumOf { it.completed }
            val totalExpected = progressList.sumOf { it.total }
            if (totalExpected > 0) totalCompleted.toFloat() / totalExpected else 0f
        } else 0f

        val launchIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1A1A2E)))
                        .padding(12.dp)
                        .clickable(actionStartActivity(launchIntent))
                ) {
                    Text(
                        "Week Progress",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9C71FF)),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(6.dp))

                    val pct = (overallProgress * 100).toInt()
                    Text(
                        "$pct%",
                        style = TextStyle(
                            color = ColorProvider(
                                when {
                                    pct >= 80 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    pct >= 50 -> androidx.compose.ui.graphics.Color(0xFFFFA726)
                                    else -> androidx.compose.ui.graphics.Color(0xFFE4E1F5)
                                }
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = overallProgress.coerceIn(0f, 1f),
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = ColorProvider(
                            when {
                                pct >= 80 -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                pct >= 50 -> androidx.compose.ui.graphics.Color(0xFFFFA726)
                                else -> androidx.compose.ui.graphics.Color(0xFF9C71FF)
                            }
                        ),
                        backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF252540))
                    )
                    Spacer(GlanceModifier.height(8.dp))

                    progressList.forEach { prog ->
                        val routinePct = if (prog.total > 0) (prog.completed * 100 / prog.total) else 0
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                prog.name,
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFE4E1F5))
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                "$routinePct%",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFC4C0DC))
                                )
                            )
                        }
                    }

                    if (progressList.isEmpty()) {
                        Text(
                            "No routines tracked yet",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFC4C0DC))
                            )
                        )
                    }
                }
            }
        }
    }
}

class WeekProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekProgressWidget()
}
