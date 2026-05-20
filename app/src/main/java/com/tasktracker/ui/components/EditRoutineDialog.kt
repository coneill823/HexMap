package com.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.Tag

private val COLOR_PALETTE = listOf(
    "#9C71FF", "#03DAC6", "#FF8A65", "#4CAF50",
    "#2196F3", "#E91E63", "#FFEB3B", "#9E9E9E"
)

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
    var selectedColorHex by remember { mutableStateOf(routine.colorHex) }
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

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLOR_PALETTE.forEach { hex ->
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
                        onConfirm(
                            routine.copy(name = name.trim(), timeMinutes = timeMinutes, colorHex = selectedColorHex),
                            selectedTagIds.toList()
                        )
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
