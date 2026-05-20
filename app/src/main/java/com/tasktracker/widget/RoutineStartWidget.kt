package com.tasktracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tasktracker.MainActivity
import com.tasktracker.TaskTrackerApplication
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.Task
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private fun safeColor(hex: String, fallback: Long): androidx.compose.ui.graphics.Color = try {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    androidx.compose.ui.graphics.Color(fallback)
}

private data class RoutineWithAvg(val routine: Routine, val avgLabel: String)

class RoutineStartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TaskTrackerApplication
        val today = LocalDate.now().toEpochDay()

        val allRoutines = try {
            app.routineRepository.getAllRoutinesWithItems().first().map { it.routine }
        } catch (_: Exception) { emptyList() }

        val routinesWithAvg = allRoutines.map { routine ->
            val avgLabel = try {
                val itemAvgs = app.sessionLogRepository.getAverageTimePerItem(routine.id)
                if (itemAvgs.isEmpty()) "--:--"
                else {
                    val totalSecs = itemAvgs.sumOf { it.avgSeconds.toDouble() }.toInt()
                    "%d:%02d".format(totalSecs / 60, totalSecs % 60)
                }
            } catch (_: Exception) { "--:--" }
            RoutineWithAvg(routine, avgLabel)
        }

        val todayTasks = try {
            val allTasks = app.taskRepository.getAllTasksWithTags().first()
            allTasks
                .filter { twt ->
                    val task = twt.task
                    !task.isCompleted && (task.scheduledDate == today || task.scheduledDate == null || task.scheduledDate == 0L)
                }
                .map { it.task }
        } catch (_: Exception) { emptyList() }

        provideContent {
            GlanceTheme {
                WidgetContent(context, routinesWithAvg, todayTasks)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, routines: List<RoutineWithAvg>, tasks: List<Task>) {
    val bg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val sectionColor = androidx.compose.ui.graphics.Color(0xFF9C71FF)
    val empty = androidx.compose.ui.graphics.Color(0xFFC4C0DC)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bg))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (tasks.isEmpty() && routines.isEmpty()) {
            Text("Nothing for today.", style = TextStyle(color = ColorProvider(empty)))
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                if (tasks.isNotEmpty()) {
                    item {
                        Text(
                            "Tasks",
                            style = TextStyle(color = ColorProvider(sectionColor), fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.padding(bottom = 4.dp)
                        )
                    }
                    items(tasks) { task ->
                        TaskWidgetRow(context, task)
                    }
                }
                if (routines.isNotEmpty()) {
                    item {
                        Text(
                            "Routines",
                            style = TextStyle(color = ColorProvider(sectionColor), fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(routines) { item ->
                        RoutineWidgetRow(context, item)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskWidgetRow(context: Context, task: Task) {
    val accent = safeColor(task.colorHex, 0xFF9C71FF)
    val textColor = androidx.compose.ui.graphics.Color(0xFFE4E1F5)
    val mutedColor = androidx.compose.ui.graphics.Color(0xFFC4C0DC)

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("start_task_id", task.id)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(20.dp)
                .background(ColorProvider(accent))
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            task.title,
            style = TextStyle(color = ColorProvider(textColor)),
            modifier = GlanceModifier.defaultWeight()
        )
        task.timeMinutes?.let { mins ->
            Text(
                "%d:%02d %s".format(
                    if (mins % 720 == 0) 12 else (mins / 60) % 12,
                    mins % 60,
                    if (mins < 720) "AM" else "PM"
                ),
                style = TextStyle(color = ColorProvider(mutedColor))
            )
            Spacer(GlanceModifier.width(4.dp))
        }
        Text("▶", style = TextStyle(color = ColorProvider(accent)))
    }
}

@Composable
private fun RoutineWidgetRow(context: Context, item: RoutineWithAvg) {
    val accent = safeColor(item.routine.colorHex, 0xFF9C71FF)
    val textColor = androidx.compose.ui.graphics.Color(0xFFE4E1F5)
    val mutedColor = androidx.compose.ui.graphics.Color(0xFFC4C0DC)

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("start_routine_id", item.routine.id)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(actionStartActivity(intent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(20.dp)
                .background(ColorProvider(accent))
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            item.routine.name,
            style = TextStyle(color = ColorProvider(textColor)),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            item.avgLabel,
            style = TextStyle(
                color = ColorProvider(if (item.avgLabel == "--:--") mutedColor else accent)
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        Text("▶", style = TextStyle(color = ColorProvider(accent)))
    }
}

class RoutineStartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RoutineStartWidget()
}
