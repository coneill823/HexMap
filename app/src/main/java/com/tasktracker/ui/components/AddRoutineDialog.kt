package com.tasktracker.ui.components

import androidx.compose.foundation.layout.*
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
    onConfirm: (Routine, List<RoutineItem>, RecurrenceDraft) -> Unit
) {
    var routineName by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<RoutineItemDraft>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var routineTimeMinutes by remember { mutableStateOf<Int?>(null) }
    var recurrenceDraft by remember { mutableStateOf(RecurrenceDraft()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Routine") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 540.dp)
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

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(routineTimeMinutes?.let { formatTime(it) } ?: "Set Routine Time (optional)")
                }
                if (routineTimeMinutes != null) {
                    TextButton(onClick = { routineTimeMinutes = null }) { Text("Clear time") }
                }

                HorizontalDivider()

                RecurrencePickerSection(
                    draft = recurrenceDraft,
                    onDraftChange = { recurrenceDraft = it }
                )

                HorizontalDivider()

                Text("Tasks in this Routine", style = MaterialTheme.typography.titleSmall)

                items.forEachIndexed { index, draft ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { title ->
                                items = items.toMutableList().also { it[index] = draft.copy(title = title) }
                            },
                            label = { Text("Task name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { items = items.toMutableList().also { it.removeAt(index) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
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
                    val routine = Routine(
                        name = routineName.trim(),
                        timeMinutes = routineTimeMinutes
                    )
                    val routineItems = items
                        .filter { it.title.isNotBlank() }
                        .mapIndexed { i, draft ->
                            RoutineItem(routineId = 0L, title = draft.title.trim(), orderIndex = i)
                        }
                    onConfirm(routine, routineItems, recurrenceDraft)
                },
                enabled = routineName.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showTimePicker) {
        TimePickerDialog(
            initialMinutes = routineTimeMinutes ?: 480,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes -> routineTimeMinutes = minutes; showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemToRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (RoutineItem) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task to Routine") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(RoutineItem(routineId = 0L, title = title.trim())) },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
