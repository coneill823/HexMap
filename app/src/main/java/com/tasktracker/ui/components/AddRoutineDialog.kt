package com.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem

data class RoutineItemDraft(
    val title: String = "",
    val description: String = "",
    val timeMinutes: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (Routine, List<RoutineItem>) -> Unit
) {
    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<RoutineItemDraft>()) }
    var timePickerForIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Routine") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name *") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = routineDescription,
                    onValueChange = { routineDescription = it },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                HorizontalDivider()

                Text("Tasks in this Routine", style = MaterialTheme.typography.titleSmall)

                items.forEachIndexed { index, draft ->
                    RoutineItemDraftRow(
                        draft = draft,
                        onTitleChange = { title ->
                            items = items.toMutableList().also { it[index] = draft.copy(title = title) }
                        },
                        onDescriptionChange = { desc ->
                            items = items.toMutableList().also { it[index] = draft.copy(description = desc) }
                        },
                        onSetTime = { timePickerForIndex = index },
                        onDelete = {
                            items = items.toMutableList().also { it.removeAt(index) }
                        }
                    )
                }

                OutlinedButton(
                    onClick = { items = items + RoutineItemDraft() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Task")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val routine = Routine(name = routineName.trim(), description = routineDescription.trim())
                    val routineItems = items
                        .filter { it.title.isNotBlank() }
                        .mapIndexed { i, draft ->
                            RoutineItem(
                                routineId = 0L,
                                title = draft.title.trim(),
                                description = draft.description.trim(),
                                timeMinutes = draft.timeMinutes,
                                orderIndex = i
                            )
                        }
                    onConfirm(routine, routineItems)
                },
                enabled = routineName.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    timePickerForIndex?.let { idx ->
        TimePickerDialog(
            initialMinutes = items[idx].timeMinutes ?: 480,
            onDismiss = { timePickerForIndex = null },
            onConfirm = { minutes ->
                items = items.toMutableList().also {
                    it[idx] = it[idx].copy(timeMinutes = minutes)
                }
                timePickerForIndex = null
            }
        )
    }
}

@Composable
private fun RoutineItemDraftRow(
    draft: RoutineItemDraft,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSetTime: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = { Text("Task title") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = draft.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onSetTime) {
                    Text(draft.timeMinutes?.let { formatTime(it) } ?: "Set time")
                }
                if (draft.timeMinutes != null) {
                    TextButton(onClick = { /* handled via callback */ }) { }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemToRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (RoutineItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timeMinutes by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task to Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(timeMinutes?.let { formatTime(it) } ?: "Set Time (optional)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        RoutineItem(
                            routineId = 0L,
                            title = title.trim(),
                            description = description.trim(),
                            timeMinutes = timeMinutes
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showTimePicker) {
        TimePickerDialog(
            initialMinutes = timeMinutes ?: 480,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                timeMinutes = minutes
                showTimePicker = false
            }
        )
    }
}
