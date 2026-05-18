package com.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.ui.theme.TAG_COLORS

@Composable
fun ManageTagsDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onCreateTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit
) {
    var showCreateTag by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(TAG_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tags.isEmpty()) {
                    Text(
                        "No tags yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tags) { tag ->
                            TagRow(tag = tag, onDelete = { onDeleteTag(tag) })
                        }
                    }
                }

                HorizontalDivider()

                if (showCreateTag) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            label = { Text("Tag name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Color", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TAG_COLORS) { colorHex ->
                                val selected = colorHex == newTagColor
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(
                                            2.dp,
                                            if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { newTagColor = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showCreateTag = false
                                    newTagName = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    if (newTagName.isNotBlank()) {
                                        onCreateTag(Tag(name = newTagName.trim(), colorHex = newTagColor))
                                        newTagName = ""
                                        newTagColor = TAG_COLORS[0]
                                        showCreateTag = false
                                    }
                                },
                                enabled = newTagName.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Create") }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showCreateTag = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Tag")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun TagRow(tag: Tag, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
            )
            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete tag",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
