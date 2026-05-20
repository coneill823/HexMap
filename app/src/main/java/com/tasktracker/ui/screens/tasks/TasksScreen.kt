package com.tasktracker.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.models.RoutineItemWithCompletion
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.ui.components.AddTaskDialog
import com.tasktracker.ui.components.EditRoutineItemDialog
import com.tasktracker.ui.components.ManageTagsDialog
import com.tasktracker.ui.components.SessionOverlayDialog
import com.tasktracker.ui.components.formatTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editingRoutineId by viewModel.editingRoutineId.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskWithTags?>(null) }
    var routineToDelete by remember { mutableStateOf<RoutineWithProgress?>(null) }
    var routineItemToDelete by remember { mutableStateOf<com.tasktracker.data.database.entities.RoutineItem?>(null) }

    // Auto-switch to Routines tab when editingRoutineId becomes non-null
    LaunchedEffect(editingRoutineId) {
        if (editingRoutineId != null) {
            selectedTabIndex = 1
            viewModel.clearRoutineToEdit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Routines") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (selectedTabIndex == 0) {
                        IconButton(onClick = { viewModel.showManageTagsDialog() }) {
                            Icon(Icons.Default.Label, contentDescription = "Manage Tags")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTabIndex) {
                0 -> FloatingActionButton(onClick = { viewModel.showAddTaskDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
                1 -> FloatingActionButton(onClick = { showAddRoutineDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Routine")
                }
                else -> {}
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Tasks") },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Routines") },
                    icon = {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> TasksTab(
                    state = state,
                    onSelectTag = viewModel::selectTag,
                    onToggleComplete = viewModel::toggleTaskComplete,
                    onEdit = viewModel::showEditDialog,
                    onDelete = { taskToDelete = it }
                )
                1 -> RoutinesTab(
                    routines = state.routines,
                    onStartSession = viewModel::startSession,
                    onToggleItem = { routineId, itemId, completed ->
                        viewModel.toggleRoutineItemComplete(routineId, itemId, completed)
                    },
                    onEditItem = viewModel::showEditRoutineItemDialog,
                    onDeleteItem = { item -> routineItemToDelete = item },
                    onDeleteRoutine = { routineToDelete = it },
                    onEditRoutine = { viewModel.showEditRoutineDialog(it.routine) },
                    onReorderItem = viewModel::reorderItem
                )
            }
        }
    }

    // Dialogs
    if (state.showAddTaskDialog) {
        AddTaskDialog(
            tags = state.tags,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = { task, tagIds, recurrence -> viewModel.saveTask(task, tagIds, recurrence) },
            onCreateTag = viewModel::saveTag
        )
    }

    state.editingTask?.let { editing ->
        AddTaskDialog(
            tags = state.tags,
            editingTask = editing,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = { task, tagIds, recurrence -> viewModel.saveTask(task, tagIds, recurrence) },
            onCreateTag = viewModel::saveTag
        )
    }

    state.editingRoutineItem?.let { item ->
        EditRoutineItemDialog(
            item = item,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = viewModel::saveRoutineItem
        )
    }

    if (state.showManageTagsDialog) {
        ManageTagsDialog(
            tags = state.tags,
            onDismiss = viewModel::dismissDialogs,
            onCreateTag = viewModel::saveTag,
            onDeleteTag = viewModel::deleteTag
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

    if (showAddRoutineDialog) {
        com.tasktracker.ui.components.AddRoutineDialog(
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { routine, items, recurrence ->
                viewModel.addRoutine(routine, items, recurrence)
                showAddRoutineDialog = false
            }
        )
    }

    state.editingRoutine?.let { routine ->
        com.tasktracker.ui.components.EditRoutineDialog(
            routine = routine,
            onDismiss = viewModel::dismissDialogs,
            onConfirm = viewModel::saveEditedRoutine
        )
    }

    // Task delete confirmation
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Delete \"${task.task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTask(task); taskToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text("Cancel") } }
        )
    }

    // Routine delete confirmation
    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine") },
            text = { Text("Delete \"${routine.routine.name}\"? It can be recovered from Options for 7 days.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteRoutine(routine.routine); routineToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { routineToDelete = null }) { Text("Cancel") } }
        )
    }

    // Routine item delete confirmation
    routineItemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { routineItemToDelete = null },
            title = { Text("Remove Item") },
            text = { Text("Remove \"${item.title}\" from this routine?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteRoutineItem(item); routineItemToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { routineItemToDelete = null }) { Text("Cancel") } }
        )
    }
}

// ---------------------------------------------------------------------------
// Tasks tab
// ---------------------------------------------------------------------------

@Composable
private fun TasksTab(
    state: TasksUiState,
    onSelectTag: (Long?) -> Unit,
    onToggleComplete: (TaskWithTags) -> Unit,
    onEdit: (TaskWithTags) -> Unit,
    onDelete: (TaskWithTags) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedTagId == null,
                    onClick = { onSelectTag(null) },
                    label = { Text("All") },
                    leadingIcon = if (state.selectedTagId == null) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
            items(state.tags) { tag ->
                val selected = tag.id == state.selectedTagId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTag(if (selected) null else tag.id) },
                    label = { Text(tag.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(
                            android.graphics.Color.parseColor(tag.colorHex)
                        ).copy(alpha = 0.2f),
                        selectedLabelColor = Color(
                            android.graphics.Color.parseColor(tag.colorHex)
                        )
                    )
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        if (state.tasks.isEmpty()) {
            EmptyTasksContent()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(state.tasks, key = { it.task.id }) { taskWithTags ->
                    TaskListItem(
                        taskWithTags = taskWithTags,
                        onToggleComplete = { onToggleComplete(taskWithTags) },
                        onEdit = { onEdit(taskWithTags) },
                        onDelete = { onDelete(taskWithTags) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Routines tab
// ---------------------------------------------------------------------------

@Composable
private fun RoutinesTab(
    routines: List<RoutineWithProgress>,
    onStartSession: (RoutineWithProgress) -> Unit,
    onToggleItem: (Long, Long, Boolean) -> Unit,
    onEditItem: (RoutineItem) -> Unit,
    onDeleteItem: (RoutineItem) -> Unit,
    onDeleteRoutine: (RoutineWithProgress) -> Unit,
    onEditRoutine: (RoutineWithProgress) -> Unit,
    onReorderItem: (Long, Int, Int) -> Unit
) {
    if (routines.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No routines yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Create routines from the Calendar tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(routines, key = { it.routine.id }) { routine ->
            RoutineManagementCard(
                routine = routine,
                onStartSession = { onStartSession(routine) },
                onToggleItem = { itemId, completed ->
                    onToggleItem(routine.routine.id, itemId, completed)
                },
                onEditItem = onEditItem,
                onDeleteItem = onDeleteItem,
                onDeleteRoutine = { onDeleteRoutine(routine) },
                onEditRoutine = { onEditRoutine(routine) },
                onReorderItem = { fromIndex, toIndex ->
                    onReorderItem(routine.routine.id, fromIndex, toIndex)
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Routine card with drag-to-reorder items
// ---------------------------------------------------------------------------

@Composable
private fun RoutineManagementCard(
    routine: RoutineWithProgress,
    onStartSession: () -> Unit,
    onToggleItem: (Long, Boolean) -> Unit,
    onEditItem: (RoutineItem) -> Unit,
    onDeleteItem: (RoutineItem) -> Unit,
    onDeleteRoutine: () -> Unit,
    onEditRoutine: () -> Unit,
    onReorderItem: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val items = routine.items

    // Drag-to-reorder state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 52.dp.toPx() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ---- Header ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        routine.routine.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    // Scheduled time badge
                    routine.routine.timeMinutes?.let { minutes ->
                        Text(
                            formatTime(minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (routine.routine.description.isNotBlank()) {
                        Text(
                            routine.routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (routine.totalCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { routine.progress },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Text(
                                "${routine.completedCount}/${routine.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Start session button (Play icon)
                IconButton(onClick = onStartSession, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start Session",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Expand / collapse button
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Edit routine button
                IconButton(onClick = onEditRoutine, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Routine",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete routine button
                IconButton(onClick = onDeleteRoutine, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Routine",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Items with drag-to-reorder ----
            if (expanded) {
                Spacer(Modifier.height(8.dp))

                if (items.isEmpty()) {
                    Text(
                        "No tasks in this routine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    // Precompute drag target index for slide offsets
                    val dragging = draggingIndex ?: -1
                    val targetIdx = if (dragging >= 0) {
                        (dragging + (dragOffsetY / itemHeightPx).roundToInt())
                            .coerceIn(0, items.size - 1)
                    } else {
                        -1
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            items.forEachIndexed { index, itemWithCompletion ->
                                val isDragging = draggingIndex == index

                                val slideOffset = when {
                                    dragging < 0 || index == dragging -> 0f
                                    dragging < index && index <= targetIdx -> -itemHeightPx
                                    dragging > index && index >= targetIdx -> itemHeightPx
                                    else -> 0f
                                }

                                RoutineItemRow(
                                    itemWithCompletion = itemWithCompletion,
                                    isDragging = isDragging,
                                    dragOffsetY = if (isDragging) dragOffsetY else 0f,
                                    slideOffset = slideOffset,
                                    onToggle = {
                                        onToggleItem(
                                            itemWithCompletion.item.id,
                                            itemWithCompletion.isCompleted
                                        )
                                    },
                                    onEdit = { onEditItem(itemWithCompletion.item) },
                                    onDelete = { onDeleteItem(itemWithCompletion.item) },
                                    dragModifier = Modifier.pointerInput(itemWithCompletion.item.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = index
                                                dragOffsetY = 0f
                                                dragOffsetX = 0f
                                            },
                                            onDrag = { _, amount ->
                                                dragOffsetY += amount.y
                                                dragOffsetX += amount.x
                                            },
                                            onDragEnd = {
                                                val xThresh = 100.dp.toPx()
                                                if (abs(dragOffsetX) > xThresh) {
                                                    onDeleteItem(itemWithCompletion.item)
                                                } else {
                                                    val newIdx = (index + (dragOffsetY / itemHeightPx)
                                                        .roundToInt())
                                                        .coerceIn(items.indices)
                                                    if (newIdx != index) {
                                                        onReorderItem(index, newIdx)
                                                    }
                                                }
                                                draggingIndex = null
                                                dragOffsetY = 0f
                                                dragOffsetX = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                dragOffsetY = 0f
                                                dragOffsetX = 0f
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineItemRow(
    itemWithCompletion: RoutineItemWithCompletion,
    isDragging: Boolean,
    dragOffsetY: Float,
    slideOffset: Float,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier
) {
    val item = itemWithCompletion.item

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else slideOffset
                shadowElevation = if (isDragging) 16f else 0f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .then(dragModifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle hint
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            modifier = Modifier
                .size(20.dp)
                .padding(end = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Checkbox(
            checked = itemWithCompletion.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(32.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (itemWithCompletion.isCompleted)
                    TextDecoration.LineThrough
                else
                    TextDecoration.None,
                color = if (itemWithCompletion.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.timeMinutes != null) {
                Text(
                    formatTime(item.timeMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Edit icon → showEditRoutineItemDialog
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

// ---------------------------------------------------------------------------
// Task list item
// ---------------------------------------------------------------------------

@Composable
private fun TaskListItem(
    taskWithTags: TaskWithTags,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val task = taskWithTags.task
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.isCompleted)
                        TextDecoration.LineThrough
                    else
                        TextDecoration.None,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatTime(task.timeMinutes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Due date badge
                    if (task.dueDate != null) {
                        val dueDate = java.time.LocalDate.ofEpochDay(task.dueDate)
                        val today = LocalDate.now()
                        val dueColor = when {
                            dueDate.isBefore(today) -> MaterialTheme.colorScheme.error
                            dueDate == today -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = dueColor
                            )
                            Text(
                                "Due ${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                                style = MaterialTheme.typography.labelMedium,
                                color = dueColor
                            )
                        }
                    }
                    taskWithTags.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Color(android.graphics.Color.parseColor(tag.colorHex))
                                        .copy(alpha = 0.25f)
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
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyTasksContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No tasks yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap + to add your first task",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
