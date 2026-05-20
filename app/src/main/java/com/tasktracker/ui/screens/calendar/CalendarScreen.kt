package com.tasktracker.ui.screens.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.ui.components.AddRoutineDialog
import com.tasktracker.ui.components.AddTaskDialog
import com.tasktracker.ui.components.EditRoutineItemDialog
import com.tasktracker.ui.components.SessionOverlayDialog
import com.tasktracker.ui.components.formatTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Data model for the Today timeline
// ---------------------------------------------------------------------------

private sealed class TimelineEntry {
    data class TaskEntry(val taskWithTags: TaskWithTags) : TimelineEntry()
    data class RoutineEntry(val routineWithProgress: RoutineWithProgress) : TimelineEntry()
    object CurrentTimeDivider : TimelineEntry()
    object UnscheduledHeader : TimelineEntry()
}

// ---------------------------------------------------------------------------
// CalendarScreen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onNavigateToRoutine: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTabIndex == 0) "Today" else "Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddTaskDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Today") },
                    icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Calendar") },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTabIndex) {
                0 -> TodayTab(
                    state = state,
                    onNavigateToRoutine = onNavigateToRoutine,
                    onStartSession = { viewModel.startSession(it) }
                )
                1 -> CalendarTab(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }

    // Dialogs
    if (state.showAddTaskDialog) {
        AddTaskDialog(
            tags = state.availableTags,
            selectedDate = state.selectedDate,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = viewModel::addTask,
            onCreateTag = viewModel::createTag
        )
    }

    if (state.showAddRoutineDialog) {
        AddRoutineDialog(
            onDismiss = viewModel::dismissDialogs,
            onConfirm = viewModel::addRoutine
        )
    }

    state.editingRoutineItem?.let { item ->
        EditRoutineItemDialog(
            item = item,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = viewModel::saveEditedRoutineItem
        )
    }

    state.sessionState?.let { session ->
        SessionOverlayDialog(
            state = session,
            onNext = viewModel::sessionNext,
            onFinish = viewModel::sessionFinish,
            onDismiss = viewModel::dismissSession
        )
    }
}

// ---------------------------------------------------------------------------
// Today Tab — scrollable timeline
// ---------------------------------------------------------------------------

@Composable
private fun TodayTab(
    state: CalendarUiState,
    onNavigateToRoutine: (Long) -> Unit,
    onStartSession: (RoutineWithProgress) -> Unit
) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute

    // Build timeline entries
    val entries = remember(state.tasks, state.routines, currentMinutes) {
        buildTimelineEntries(state.tasks, state.routines, currentMinutes)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to current time position when tab loads
    LaunchedEffect(Unit) {
        val dividerIndex = entries.indexOfFirst { it is TimelineEntry.CurrentTimeDivider }
        if (dividerIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(index = dividerIndex.coerceAtLeast(0))
            }
        }
    }

    val today = LocalDate.now()
    val isToday = state.selectedDate == today

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (!isToday) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Showing today's tasks. Select today in Calendar tab to see your schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        items(entries, key = { entry ->
            when (entry) {
                is TimelineEntry.TaskEntry -> "task_${entry.taskWithTags.task.id}"
                is TimelineEntry.RoutineEntry -> "routine_${entry.routineWithProgress.routine.id}"
                is TimelineEntry.CurrentTimeDivider -> "current_time_divider"
                is TimelineEntry.UnscheduledHeader -> "unscheduled_header"
            }
        }) { entry ->
            when (entry) {
                is TimelineEntry.UnscheduledHeader -> {
                    UnscheduledSectionHeader()
                }
                is TimelineEntry.CurrentTimeDivider -> {
                    CurrentTimeDivider(currentMinutes = currentMinutes)
                }
                is TimelineEntry.TaskEntry -> {
                    TimelineTaskRow(
                        taskWithTags = entry.taskWithTags
                    )
                }
                is TimelineEntry.RoutineEntry -> {
                    TimelineRoutineCard(
                        routineWithProgress = entry.routineWithProgress,
                        onStartSession = { onStartSession(entry.routineWithProgress) },
                        onNavigateToRoutine = { onNavigateToRoutine(entry.routineWithProgress.routine.id) }
                    )
                }
            }
        }

        if (entries.none { it is TimelineEntry.TaskEntry || it is TimelineEntry.RoutineEntry }) {
            item {
                EmptyTodayContent()
            }
        }
    }
}

private fun buildTimelineEntries(
    tasks: List<TaskWithTags>,
    routines: List<RoutineWithProgress>,
    currentMinutes: Int
): List<TimelineEntry> {
    val result = mutableListOf<TimelineEntry>()

    // Separate scheduled vs unscheduled
    val unscheduledTasks = tasks.filter { it.task.timeMinutes == null }
    val scheduledTasks = tasks.filter { it.task.timeMinutes != null }
    val unscheduledRoutines = routines.filter { it.routine.timeMinutes == null }
    val scheduledRoutines = routines.filter { it.routine.timeMinutes != null }

    // Unscheduled section
    val hasUnscheduled = unscheduledTasks.isNotEmpty() || unscheduledRoutines.isNotEmpty()
    if (hasUnscheduled) {
        result.add(TimelineEntry.UnscheduledHeader)
        unscheduledTasks.forEach { result.add(TimelineEntry.TaskEntry(it)) }
        unscheduledRoutines.forEach { result.add(TimelineEntry.RoutineEntry(it)) }
    }

    // Merge scheduled items sorted by time
    data class ScheduledItem(val timeMinutes: Int, val entry: TimelineEntry)

    val scheduledItems = mutableListOf<ScheduledItem>()
    scheduledTasks.forEach { twt ->
        scheduledItems.add(ScheduledItem(twt.task.timeMinutes!!, TimelineEntry.TaskEntry(twt)))
    }
    scheduledRoutines.forEach { rwp ->
        scheduledItems.add(ScheduledItem(rwp.routine.timeMinutes!!, TimelineEntry.RoutineEntry(rwp)))
    }
    scheduledItems.sortBy { it.timeMinutes }

    // Inject current-time divider at the right position
    var dividerInserted = false
    for (item in scheduledItems) {
        if (!dividerInserted && item.timeMinutes > currentMinutes) {
            result.add(TimelineEntry.CurrentTimeDivider)
            dividerInserted = true
        }
        result.add(item.entry)
    }
    // If all scheduled items are before current time (or none), append divider at end
    if (!dividerInserted) {
        result.add(TimelineEntry.CurrentTimeDivider)
    }

    return result
}

@Composable
private fun UnscheduledSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Unscheduled",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CurrentTimeDivider(currentMinutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.error,
            thickness = 1.5.dp
        )
        Text(
            text = formatTime(currentMinutes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}

@Composable
private fun TimelineTaskRow(
    taskWithTags: TaskWithTags
) {
    val task = taskWithTags.task
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time on the left
        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = task.timeMinutes?.let { formatTime(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (taskWithTags.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            taskWithTags.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.25f)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        tag.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(android.graphics.Color.parseColor(tag.colorHex))
                                    )
                                }
                            }
                        }
                    }
                }
                if (task.timeMinutes != null) {
                    Text(
                        formatTime(task.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRoutineCard(
    routineWithProgress: RoutineWithProgress,
    onStartSession: () -> Unit,
    onNavigateToRoutine: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val routine = routineWithProgress.routine

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Time on the left
        Box(
            modifier = Modifier
                .width(56.dp)
                .padding(top = 14.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = routine.timeMinutes?.let { formatTime(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (routineWithProgress.isFullyCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        routine.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (routineWithProgress.totalCount > 0) {
                        Text(
                            "${routineWithProgress.completedCount}/${routineWithProgress.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    // Play button to start session
                    IconButton(
                        onClick = onStartSession,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start Session",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Edit button → navigate to Routines tab
                    IconButton(
                        onClick = onNavigateToRoutine,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit routine",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (routineWithProgress.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { routineWithProgress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }

                if (expanded) {
                    Spacer(Modifier.height(6.dp))
                    if (routineWithProgress.items.isEmpty()) {
                        Text(
                            "No tasks in this routine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        routineWithProgress.items.forEach { itemWithCompletion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    itemWithCompletion.item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (itemWithCompletion.item.timeMinutes != null) {
                                    Text(
                                        formatTime(itemWithCompletion.item.timeMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTodayContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.WbSunny,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            "Nothing scheduled for today",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Calendar Tab — month calendar + day list (existing behaviour)
// ---------------------------------------------------------------------------

@Composable
private fun CalendarTab(
    state: CalendarUiState,
    viewModel: CalendarViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Day navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectDate(state.selectedDate.minusDays(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                }
                Text(
                    text = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                IconButton(onClick = { viewModel.selectDate(state.selectedDate.plusDays(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                }
            }
        }

        if (state.tasks.isNotEmpty()) {
            item { SectionHeader(title = "Tasks", icon = Icons.Default.CheckCircle) }
            items(state.tasks, key = { "task_${it.task.id}" }) { taskWithTags ->
                TaskCard(
                    taskWithTags = taskWithTags,
                    onToggleComplete = { viewModel.toggleTaskComplete(taskWithTags) },
                    onDelete = { viewModel.deleteTask(taskWithTags) }
                )
            }
        }

        if (state.routines.isNotEmpty()) {
            item { SectionHeader(title = "Routines", icon = Icons.Default.Repeat) }
            items(state.routines, key = { "routine_${it.routine.id}" }) { routineWithProgress ->
                RoutineCard(
                    routineWithProgress = routineWithProgress,
                    onStartSession = { viewModel.startSession(routineWithProgress) },
                    onToggleItem = { itemId, currentlyCompleted ->
                        viewModel.toggleRoutineItemComplete(
                            routineWithProgress.routine.id,
                            itemId,
                            state.selectedDate,
                            currentlyCompleted
                        )
                    },
                    onDeleteItem = { item -> viewModel.deleteRoutineItem(item) },
                    onDeleteRoutine = { viewModel.deleteRoutine(routineWithProgress.routine) },
                    onEditItem = { item -> viewModel.showEditRoutineItemDialog(item) }
                )
            }
        }

        if (state.tasks.isEmpty() && state.routines.isEmpty()) {
            item { EmptyDayContent(onAddTask = { viewModel.showAddTaskDialog() }) }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared composables
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TaskCard(
    taskWithTags: TaskWithTags,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val task = taskWithTags.task
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (task.timeMinutes != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    formatTime(task.timeMinutes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    taskWithTags.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                tag.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(android.graphics.Color.parseColor(tag.colorHex))
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routineWithProgress: RoutineWithProgress,
    onStartSession: () -> Unit,
    onToggleItem: (Long, Boolean) -> Unit,
    onDeleteItem: (RoutineItem) -> Unit,
    onDeleteRoutine: () -> Unit,
    onEditItem: (RoutineItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (routineWithProgress.isFullyCompleted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(routineWithProgress.routine.name, style = MaterialTheme.typography.titleSmall)
                    }
                    if (routineWithProgress.totalCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { routineWithProgress.progress },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "${routineWithProgress.completedCount}/${routineWithProgress.totalCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onStartSession, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start Session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteRoutine, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete routine",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                routineWithProgress.items.forEach { itemWithCompletion ->
                    RoutineItemRow(
                        title = itemWithCompletion.item.title,
                        description = itemWithCompletion.item.description,
                        timeMinutes = itemWithCompletion.item.timeMinutes,
                        isCompleted = itemWithCompletion.isCompleted,
                        onToggle = { onToggleItem(itemWithCompletion.item.id, itemWithCompletion.isCompleted) },
                        onDelete = { onDeleteItem(itemWithCompletion.item) },
                        onEdit = { onEditItem(itemWithCompletion.item) }
                    )
                }
                if (routineWithProgress.items.isEmpty()) {
                    Text(
                        "No tasks in this routine yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineItemRow(
    title: String,
    description: String,
    timeMinutes: Int?,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(36.dp),
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (timeMinutes != null) {
            Text(
                formatTime(timeMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EmptyDayContent(onAddTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            "No tasks for this day",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onAddTask) {
            Text("Add Task")
        }
    }
}
