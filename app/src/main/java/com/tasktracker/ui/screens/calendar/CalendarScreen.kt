package com.tasktracker.ui.screens.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.database.entities.RecurrenceRule
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.ui.components.AddRoutineDialog
import com.tasktracker.ui.components.EditRoutineItemDialog
import com.tasktracker.ui.components.SessionOverlayDialog
import com.tasktracker.ui.components.formatTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

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
// Today Tab — Canvas-based timeline 6am to midnight
// ---------------------------------------------------------------------------

private val HOUR_HEIGHT = 64.dp
private const val START_HOUR = 6
private const val END_HOUR = 24
private const val TOTAL_HOURS = END_HOUR - START_HOUR // 18

@Composable
private fun TodayTab(
    state: CalendarUiState,
    onNavigateToRoutine: (Long) -> Unit,
    onStartSession: (RoutineWithProgress) -> Unit
) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute

    val unscheduledTasks = remember(state.tasks) { state.tasks.filter { it.task.timeMinutes == null } }
    val scheduledTasks = remember(state.tasks) { state.tasks.filter { it.task.timeMinutes != null } }
    val unscheduledRoutines = remember(state.routines) { state.routines.filter { it.routine.timeMinutes == null } }
    val scheduledRoutines = remember(state.routines) { state.routines.filter { it.routine.timeMinutes != null } }

    val hasUnscheduled = unscheduledTasks.isNotEmpty() || unscheduledRoutines.isNotEmpty()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to current time on initial load
    LaunchedEffect(Unit) {
        val currentHourOffset = (currentMinutes - START_HOUR * 60).coerceAtLeast(0)
        val totalMinutes = TOTAL_HOURS * 60
        if (currentHourOffset in 0..totalMinutes) {
            // We need density to compute px; approximate with 64dp per hour at 2dp/px typical
            // We'll scroll after composition settles; use a short pass-through
            coroutineScope.launch {
                // Compute scroll target: each hour is HOUR_HEIGHT dp
                // We can't access density here directly, so we use the actual scrollable height
                // The scroll will animate after the content is laid out
                val fractionOfTimeline = currentHourOffset.toFloat() / (TOTAL_HOURS * 60)
                val estimatedMaxScroll = scrollState.maxValue
                if (estimatedMaxScroll > 0) {
                    scrollState.animateScrollTo((fractionOfTimeline * estimatedMaxScroll).toInt())
                }
            }
        }
    }

    // Second effect that waits for scroll to be ready
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            val currentHourOffset = (currentMinutes - START_HOUR * 60).coerceAtLeast(0)
            val fractionOfTimeline = currentHourOffset.toFloat() / (TOTAL_HOURS * 60)
            scrollState.animateScrollTo((fractionOfTimeline * scrollState.maxValue).toInt())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Unscheduled section
        if (hasUnscheduled) {
            Text(
                text = "Unscheduled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            unscheduledTasks.forEach { twt ->
                CompactTaskCard(twt)
            }
            unscheduledRoutines.forEach { rwp ->
                CompactRoutineCard(rwp, onStartSession = { onStartSession(rwp) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        // Timeline
        val hourHeightDp = HOUR_HEIGHT
        val totalHeightDp = hourHeightDp * TOTAL_HOURS

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            val surfaceVariantColor = MaterialTheme.colorScheme.onSurface
            val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

            // 1. Background canvas — hour and half-hour lines
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp)
            ) {
                val leftPad = 56.dp.toPx()
                val hourPx = hourHeightDp.toPx()

                for (hour in 0..TOTAL_HOURS) {
                    val y = hourPx * hour
                    // Hour line
                    drawLine(
                        color = surfaceVariantColor.copy(alpha = 0.12f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    // Half-hour line
                    if (hour < TOTAL_HOURS) {
                        val yHalf = y + hourPx / 2f
                        drawLine(
                            color = surfaceVariantColor.copy(alpha = 0.05f),
                            start = Offset(leftPad, yHalf),
                            end = Offset(size.width, yHalf),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // 2. Time labels column (left side)
            Column(modifier = Modifier.width(52.dp)) {
                for (hour in 0..TOTAL_HOURS) {
                    Box(
                        modifier = Modifier.height(hourHeightDp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        val h = START_HOUR + hour
                        val label = when {
                            h == 0 -> "12am"
                            h < 12 -> "${h}am"
                            h == 12 -> "12pm"
                            h == 24 -> "12am"
                            else -> "${h - 12}pm"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariantColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                        )
                    }
                }
            }

            // 3. Scheduled tasks — absolutely positioned
            scheduledTasks.forEach { twt ->
                val timeMin = twt.task.timeMinutes ?: return@forEach
                if (timeMin < START_HOUR * 60 || timeMin >= END_HOUR * 60) return@forEach
                val yOffsetDp = ((timeMin - START_HOUR * 60) / 60f) * hourHeightDp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = yOffsetDp)
                        .padding(start = 58.dp, end = 8.dp)
                ) {
                    CompactTaskCard(twt)
                }
            }

            // 4. Scheduled routines — absolutely positioned
            scheduledRoutines.forEach { rwp ->
                val timeMin = rwp.routine.timeMinutes ?: return@forEach
                if (timeMin < START_HOUR * 60 || timeMin >= END_HOUR * 60) return@forEach
                val yOffsetDp = ((timeMin - START_HOUR * 60) / 60f) * hourHeightDp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = yOffsetDp)
                        .padding(start = 58.dp, end = 8.dp)
                ) {
                    CompactRoutineCard(rwp, onStartSession = { onStartSession(rwp) })
                }
            }

            // 5. Current time indicator (red line)
            if (currentMinutes >= START_HOUR * 60 && currentMinutes < END_HOUR * 60) {
                val yOffsetDp = ((currentMinutes - START_HOUR * 60) / 60f) * hourHeightDp
                val errorColor = MaterialTheme.colorScheme.error
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = yOffsetDp)
                ) {
                    drawLine(
                        color = errorColor,
                        start = Offset(52.dp.toPx(), 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = errorColor,
                        radius = 5.dp.toPx(),
                        center = Offset(52.dp.toPx(), 0f)
                    )
                }
            }
        }

        // Empty state when no tasks/routines at all
        if (state.tasks.isEmpty() && state.routines.isEmpty()) {
            EmptyTodayContent()
        }
    }
}

// ---------------------------------------------------------------------------
// Compact cards for the Today tab
// ---------------------------------------------------------------------------

@Composable
private fun CompactTaskCard(taskWithTags: TaskWithTags) {
    val task = taskWithTags.task
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactRoutineCard(
    routineWithProgress: RoutineWithProgress,
    onStartSession: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val routine = routineWithProgress.routine

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    routine.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (routineWithProgress.totalCount > 0) {
                    Text(
                        "${routineWithProgress.completedCount}/${routineWithProgress.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (routine.timeMinutes != null) {
                    Text(
                        formatTime(routine.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
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
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (expanded && routineWithProgress.items.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                routineWithProgress.items.forEach { itemWithCompletion ->
                    Text(
                        text = itemWithCompletion.item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Calendar Tab — month calendar + colored dots + sorted day list
// ---------------------------------------------------------------------------

@Composable
private fun CalendarTab(
    state: CalendarUiState,
    viewModel: CalendarViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MonthCalendar(
            currentMonth = state.currentMonth,
            selectedDate = state.selectedDate,
            allRoutines = state.allRoutines,
            routineRules = state.routineRules,
            onDaySelected = viewModel::selectDate,
            onPrevMonth = { viewModel.navigateMonth(-1) },
            onNextMonth = { viewModel.navigateMonth(1) }
        )

        HorizontalDivider()

        // Selected day header
        Text(
            text = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Day content — tasks + routines sorted by timeMinutes (null last)
        val sortedTasks = remember(state.tasks) {
            state.tasks.sortedWith(compareBy(nullsLast()) { it.task.timeMinutes })
        }
        val sortedRoutines = remember(state.routines) {
            state.routines.sortedWith(compareBy(nullsLast()) { it.routine.timeMinutes })
        }

        if (sortedTasks.isEmpty() && sortedRoutines.isEmpty()) {
            EmptyDayContent()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (sortedTasks.isNotEmpty()) {
                    item { SectionHeader(title = "Tasks", icon = Icons.Default.CheckCircle) }
                    items(sortedTasks, key = { "task_${it.task.id}" }) { taskWithTags ->
                        TaskCard(
                            taskWithTags = taskWithTags,
                            onToggleComplete = { viewModel.toggleTaskComplete(taskWithTags) },
                            onDelete = { viewModel.deleteTask(taskWithTags) }
                        )
                    }
                }

                if (sortedRoutines.isNotEmpty()) {
                    item { SectionHeader(title = "Routines", icon = Icons.Default.Repeat) }
                    items(sortedRoutines, key = { "routine_${it.routine.id}" }) { routineWithProgress ->
                        CalendarRoutineCard(
                            routineWithProgress = routineWithProgress,
                            onStartSession = { viewModel.startSession(routineWithProgress) }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Month Calendar composable
// ---------------------------------------------------------------------------

@Composable
private fun MonthCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    allRoutines: List<RoutineWithProgress>,
    routineRules: Map<Long, RecurrenceRule>,
    onDaySelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            )
            daysOfWeek.forEach { dow ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Build calendar grid
        val firstDayOfMonth = currentMonth.atDay(1)
        // Sunday = 0, Monday = 1, ... Saturday = 6 in US calendar
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday=0

        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = startDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startDayOfWeek + 1

                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(dayNumber)
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        // Compute dots: routines that occur on this date
                        val routinesForDay = allRoutines.filter { rwp ->
                            val rule = routineRules[rwp.routine.id]
                            rule?.occursOn(date) ?: true
                        }

                        CalendarDayCell(
                            day = dayNumber,
                            isToday = isToday,
                            isSelected = isSelected,
                            routinesForDay = routinesForDay,
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    routinesForDay: List<RoutineWithProgress>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            } else if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(surfaceVariantColor)
                )
            }
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isToday -> onPrimaryColor
                    isSelected -> onSurfaceColor
                    else -> onSurfaceColor
                }
            )
        }

        // Routine color dots
        if (routinesForDay.isNotEmpty()) {
            val maxDots = 5
            val dotsToShow = routinesForDay.take(maxDots)
            val remaining = routinesForDay.size - dotsToShow.size

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            ) {
                dotsToShow.forEach { rwp ->
                    val dotColor = try {
                        Color(android.graphics.Color.parseColor(rwp.routine.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF9C71FF.toInt())
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                if (remaining > 0) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Calendar tab routine card — collapsed by default, read-only items
// ---------------------------------------------------------------------------

@Composable
private fun CalendarRoutineCard(
    routineWithProgress: RoutineWithProgress,
    onStartSession: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (routineWithProgress.isFullyCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    routineWithProgress.routine.name,
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
                if (routineWithProgress.routine.timeMinutes != null) {
                    Text(
                        formatTime(routineWithProgress.routine.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
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
                        Text(
                            text = itemWithCompletion.item.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
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
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)) {
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

@Composable
private fun EmptyDayContent() {
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
    }
}
