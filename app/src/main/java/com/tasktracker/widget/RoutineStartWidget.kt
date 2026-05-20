package com.tasktracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tasktracker.TaskTrackerApplication
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.RoutineSessionLog
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

private const val PREFS_FILE = "widget_session_prefs"
private const val KEY_ROUTINE_ID = "active_routine_id"
private const val KEY_CHECKED_ITEMS = "checked_items"
private const val KEY_SESSION_START = "session_start"

private fun safeColor(hex: String, fallback: Long): androidx.compose.ui.graphics.Color = try {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    androidx.compose.ui.graphics.Color(fallback)
}

class RoutineStartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TaskTrackerApplication
        val allRoutines = try {
            app.routineRepository.getAllRoutinesWithItems().first().map { it.routine }
        } catch (_: Exception) { emptyList() }

        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val activeRoutineId = prefs.getLong(KEY_ROUTINE_ID, -1L)
        val activeRoutine = if (activeRoutineId != -1L) allRoutines.firstOrNull { it.id == activeRoutineId } else null

        val sessionItems = if (activeRoutine != null) {
            try { app.routineRepository.getItemsForRoutine(activeRoutineId) }
            catch (_: Exception) { emptyList() }
        } else emptyList<RoutineItem>()

        val checkedIds = prefs.getString(KEY_CHECKED_ITEMS, "")
            ?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toLongOrNull() }?.toSet()
            ?: emptySet()

        provideContent {
            GlanceTheme {
                if (activeRoutine != null) {
                    SessionContent(activeRoutine, sessionItems, checkedIds)
                } else {
                    RoutineListContent(allRoutines)
                }
            }
        }
    }
}

@Composable
private fun RoutineListContent(routines: List<Routine>) {
    val bg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val header = androidx.compose.ui.graphics.Color(0xFF9C71FF)
    val empty = androidx.compose.ui.graphics.Color(0xFFC4C0DC)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bg))
            .padding(12.dp)
    ) {
        Text(
            "My Routines",
            style = TextStyle(color = ColorProvider(header), fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(8.dp))
        if (routines.isEmpty()) {
            Text(
                "No routines yet.\nCreate one in the app.",
                style = TextStyle(color = ColorProvider(empty))
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(routines) { routine ->
                    RoutineListRow(routine)
                }
            }
        }
    }
}

@Composable
private fun RoutineListRow(routine: Routine) {
    val accent = safeColor(routine.colorHex, 0xFF9C71FF)
    val textColor = androidx.compose.ui.graphics.Color(0xFFE4E1F5)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(
                actionRunCallback<StartRoutineAction>(
                    actionParametersOf(StartRoutineAction.ROUTINE_ID to routine.id)
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(24.dp)
                .background(ColorProvider(accent))
        ) {}
        Spacer(GlanceModifier.width(10.dp))
        Text(
            routine.name,
            style = TextStyle(color = ColorProvider(textColor)),
            modifier = GlanceModifier.defaultWeight()
        )
        routine.timeMinutes?.let { mins ->
            Text(
                "%d:%02d".format(mins / 60, mins % 60),
                style = TextStyle(color = ColorProvider(accent))
            )
            Spacer(GlanceModifier.width(6.dp))
        }
        Text("▶", style = TextStyle(color = ColorProvider(accent)))
    }
}

@Composable
private fun SessionContent(routine: Routine, items: List<RoutineItem>, checkedIds: Set<Long>) {
    val bg = androidx.compose.ui.graphics.Color(0xFF1A1A2E)
    val accent = safeColor(routine.colorHex, 0xFF9C71FF)
    val doneColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
    val cancelColor = androidx.compose.ui.graphics.Color(0xFF888888)
    val itemText = androidx.compose.ui.graphics.Color(0xFFE4E1F5)
    val doneText = androidx.compose.ui.graphics.Color(0xFF666666)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bg))
            .padding(12.dp)
    ) {
        // Header row: name | Cancel | Done
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                routine.name,
                style = TextStyle(color = ColorProvider(accent), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                "✕",
                style = TextStyle(color = ColorProvider(cancelColor), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .padding(horizontal = 8.dp)
                    .clickable(actionRunCallback<CancelSessionAction>())
            )
            Text(
                "✓ Done",
                style = TextStyle(color = ColorProvider(doneColor), fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(
                    actionRunCallback<FinishSessionAction>(
                        actionParametersOf(FinishSessionAction.ROUTINE_ID to routine.id)
                    )
                )
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        if (items.isEmpty()) {
            Text(
                "No items in this routine.",
                style = TextStyle(color = ColorProvider(cancelColor))
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(items) { item ->
                    val checked = item.id in checkedIds
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                actionRunCallback<ToggleItemAction>(
                                    actionParametersOf(
                                        ToggleItemAction.ITEM_ID to item.id,
                                        ToggleItemAction.ROUTINE_ID to routine.id
                                    )
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (checked) "☑" else "☐",
                            style = TextStyle(
                                color = ColorProvider(
                                    if (checked) doneColor else cancelColor
                                )
                            )
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            item.title,
                            style = TextStyle(
                                color = ColorProvider(if (checked) doneText else itemText)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Action Callbacks ────────────────────────────────────────────────────────

class StartRoutineAction : ActionCallback {
    companion object {
        val ROUTINE_ID = ActionParameters.Key<Long>("routineId")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val routineId = parameters[ROUTINE_ID] ?: return
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit()
            .putLong(KEY_ROUTINE_ID, routineId)
            .putString(KEY_CHECKED_ITEMS, "")
            .putLong(KEY_SESSION_START, System.currentTimeMillis())
            .apply()
        RoutineStartWidget().update(context, glanceId)
    }
}

class ToggleItemAction : ActionCallback {
    companion object {
        val ITEM_ID = ActionParameters.Key<Long>("itemId")
        val ROUTINE_ID = ActionParameters.Key<Long>("routineId")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ITEM_ID] ?: return
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_CHECKED_ITEMS, "")
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
        val idStr = itemId.toString()
        if (idStr in current) current.remove(idStr) else current.add(idStr)
        prefs.edit().putString(KEY_CHECKED_ITEMS, current.joinToString(",")).apply()
        RoutineStartWidget().update(context, glanceId)
    }
}

class FinishSessionAction : ActionCallback {
    companion object {
        val ROUTINE_ID = ActionParameters.Key<Long>("routineId")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val routineId = parameters[ROUTINE_ID] ?: return
        val app = context.applicationContext as TaskTrackerApplication
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val checkedIds = prefs.getString(KEY_CHECKED_ITEMS, "")
            ?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        if (checkedIds.isNotEmpty()) {
            val dateEpochDay = LocalDate.now().toEpochDay()
            val sessionId = UUID.randomUUID().toString()
            // Mark items complete and save session logs
            checkedIds.forEach { itemId ->
                app.routineRepository.setItemCompletion(routineId, itemId, dateEpochDay, true)
            }
            val logs = checkedIds.map { itemId ->
                RoutineSessionLog(
                    routineId = routineId,
                    routineItemId = itemId,
                    sessionId = sessionId,
                    elapsedSeconds = 0,
                    dateEpochDay = dateEpochDay
                )
            }
            app.sessionLogRepository.saveLogs(logs)
        }

        prefs.edit()
            .remove(KEY_ROUTINE_ID)
            .remove(KEY_CHECKED_ITEMS)
            .remove(KEY_SESSION_START)
            .apply()
        RoutineStartWidget().update(context, glanceId)
    }
}

class CancelSessionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit()
            .remove(KEY_ROUTINE_ID)
            .remove(KEY_CHECKED_ITEMS)
            .remove(KEY_SESSION_START)
            .apply()
        RoutineStartWidget().update(context, glanceId)
    }
}

class RoutineStartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RoutineStartWidget()
}
