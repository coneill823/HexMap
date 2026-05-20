package com.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineDialog(
    routine: Routine,
    availableTags: List<Tag> = emptyList(),
    initialTagIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Routine, List<Long>) -> Unit
) {
    var name by remember { mutableStateOf(routine.name) }
    var timeMinutes by remember { mutableStateOf(routine.timeMinutes) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(initialTagIds.toSet()) }
    val timePickerState = rememberTimePickerState(
        initialHour = routine.timeMinutes?.div(60) ?: 8,
        initialMinute = routine.timeMinutes?.rem(60) ?: 0
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    timeMinutes = timePickerState.hour * 60 + timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = timeMinutes?.let { formatTime(it) } ?: "No time set",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showTimePicker = true }) { Text("Set Time") }
                    if (timeMinutes != null) {
                        TextButton(onClick = { timeMinutes = null }) { Text("Clear") }
                    }
                }

                if (availableTags.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Tags", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableTags) { tag ->
                            val selected = tag.id in selectedTagIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                                },
                                label = { Text(tag.name) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(routine.copy(name = name.trim(), timeMinutes = timeMinutes), selectedTagIds.toList())
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
