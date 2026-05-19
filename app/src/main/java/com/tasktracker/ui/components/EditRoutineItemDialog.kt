package com.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.RoutineItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineItemDialog(
    item: RoutineItem,
    onDismiss: () -> Unit,
    onConfirm: (RoutineItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description) }
    var timeMinutes by remember { mutableStateOf(item.timeMinutes) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
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
                    maxLines = 3
                )
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(item.copy(title = title.trim(), description = description.trim(), timeMinutes = timeMinutes))
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
            onConfirm = { minutes ->
                timeMinutes = minutes
                showTimePicker = false
            }
        )
    }
}
