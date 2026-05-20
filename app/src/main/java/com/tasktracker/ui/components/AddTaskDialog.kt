package com.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tasktracker.work.ReminderScheduler
import com.tasktracker.data.database.entities.RecurrenceRule
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.models.TaskWithTags
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    tags: List<Tag>,
    selectedDate: LocalDate? = null,
    editingTask: TaskWithTags? = null,
    onDismiss: () -> Unit,
    onConfirm: (Task, List<Long>, RecurrenceDraft) -> Unit,
    onCreateTag: (Tag) -> Unit
) {
    var title by remember { mutableStateOf(editingTask?.task?.title ?: "") }
    var description by remember { mutableStateOf(editingTask?.task?.description ?: "") }
    var selectedTagIds by remember {
        mutableStateOf(editingTask?.tags?.map { it.id }?.toSet() ?: emptySet())
    }
    var timeMinutes by remember { mutableStateOf(editingTask?.task?.timeMinutes) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dueDateEpochDay by remember { mutableStateOf(editingTask?.task?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedReminderMinutes by remember { mutableStateOf(editingTask?.task?.reminderDaysBefore) }
    var showReminderDropdown by remember { mutableStateOf(false) }
    var recurrenceDraft by remember { mutableStateOf(RecurrenceDraft()) }
    var selectedColorHex by remember { mutableStateOf(editingTask?.task?.colorHex ?: "#9C71FF") }

    val titleError = title.isBlank()

    val colorPalette = listOf(
        "#9C71FF", "#03DAC6", "#FF8A65", "#4CAF50",
        "#2196F3", "#E91E63", "#FFEB3B", "#9E9E9E"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingTask != null) "Edit Task" else "Add Task") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    isError = titleError && title.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorPalette.forEach { hex ->
                        val selected = hex == selectedColorHex
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape) else Modifier)
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Time picker button
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(timeMinutes?.let { formatTime(it) } ?: "Set Time (optional)")
                }
                if (timeMinutes != null) {
                    TextButton(onClick = { timeMinutes = null }) { Text("Clear time") }
                }

                // Due date picker
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dueDateEpochDay?.let { "Due: ${LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}" }
                            ?: "Set Due Date (optional)"
                    )
                }
                if (dueDateEpochDay != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { dueDateEpochDay = null; selectedReminderMinutes = null }) { Text("Clear due date") }
                    }
                    // Reminder dropdown
                    Box {
                        OutlinedButton(
                            onClick = { showReminderDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                selectedReminderMinutes?.let { m ->
                                    ReminderScheduler.REMINDER_OPTIONS.find { it.first == m }?.second ?: "Custom"
                                } ?: "Set Reminder (optional)"
                            )
                        }
                        DropdownMenu(
                            expanded = showReminderDropdown,
                            onDismissRequest = { showReminderDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No reminder") },
                                onClick = { selectedReminderMinutes = null; showReminderDropdown = false }
                            )
                            ReminderScheduler.REMINDER_OPTIONS.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { selectedReminderMinutes = minutes; showReminderDropdown = false },
                                    trailingIcon = if (selectedReminderMinutes == minutes) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    Text("Tags", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            val selected = tag.id in selectedTagIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                                },
                                label = { Text(tag.name) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.3f),
                                    selectedLabelColor = Color(android.graphics.Color.parseColor(tag.colorHex))
                                )
                            )
                        }
                    }
                }

                if (selectedDate != null) {
                    Text(
                        "Scheduled: ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                RecurrencePickerSection(
                    draft = recurrenceDraft,
                    onDraftChange = { recurrenceDraft = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!titleError) {
                        val task = Task(
                            id = editingTask?.task?.id ?: 0L,
                            title = title.trim(),
                            description = description.trim(),
                            timeMinutes = timeMinutes,
                            scheduledDate = selectedDate?.toEpochDay() ?: editingTask?.task?.scheduledDate,
                            isCompleted = editingTask?.task?.isCompleted ?: false,
                            createdAt = editingTask?.task?.createdAt ?: System.currentTimeMillis(),
                            dueDate = dueDateEpochDay,
                            reminderDaysBefore = selectedReminderMinutes,
                            reminderWorkerId = editingTask?.task?.reminderWorkerId,
                            colorHex = selectedColorHex
                        )
                        onConfirm(task, selectedTagIds.toList(), recurrenceDraft)
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showTimePicker) {
        TimePickerDialog(
            initialMinutes = timeMinutes ?: 480,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes -> timeMinutes = minutes; showTimePicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateEpochDay?.let { it * 86400000L }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateEpochDay = datePickerState.selectedDateMillis?.let { it / 86400000L }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val ampm = if (h < 12) "AM" else "PM"
    val displayH = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(displayH, m, ampm)
}
