package com.tasktracker.ui.screens.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LINE_COLORS = listOf(
    Color(0xFF9C71FF), Color(0xFF03DAC6), Color(0xFFFF8A65), Color(0xFF4CAF50),
    Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFFEB3B), Color(0xFF9C27B0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphsScreen(viewModel: GraphsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graphs") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 12.dp)
        ) {
            item {
                // Routine selector
                var expanded by remember { mutableStateOf(false) }
                val selectedName = state.allRoutines
                    .firstOrNull { it.id == state.selectedRoutineId }?.name
                    ?: "Select a Routine"

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Routine") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.allRoutines.forEach { routine ->
                            DropdownMenuItem(
                                text = { Text(routine.name) },
                                onClick = { viewModel.selectRoutine(routine.id); expanded = false }
                            )
                        }
                    }
                }
            }

            if (state.selectedRoutineId == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Select a routine to see how your speed\nimproves over time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (state.sessionPoints.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No session data yet.\nComplete this routine using the Play button\nto see your trend line.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    // Chart title
                    Text(
                        "Time per item (seconds) — lower is faster",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    val itemIds = state.sessionPoints.map { it.routineItemId }.distinct()
                    val dates = state.sessionPoints.map { it.dateEpochDay }.distinct().sorted()
                    val dataByItem = itemIds.associateWith { itemId ->
                        state.sessionPoints.filter { it.routineItemId == itemId }
                            .associate { it.dateEpochDay to it.avgSeconds }
                    }
                    val maxY = state.sessionPoints.maxOf { it.avgSeconds }.coerceAtLeast(10f)
                    val dateFmt = DateTimeFormatter.ofPattern("M/d")

                    Card(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val w = constraints.maxWidth.toFloat()
                            val h = constraints.maxHeight.toFloat()
                            val leftPad = 56f
                            val bottomPad = 40f
                            val chartW = (w - leftPad - 8f).coerceAtLeast(1f)
                            val chartH = (h - bottomPad - 8f).coerceAtLeast(1f)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridCount = 4
                                // Grid lines + Y axis labels
                                for (i in 0..gridCount) {
                                    val y = 8f + chartH * (1f - i.toFloat() / gridCount)
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.08f),
                                        start = Offset(leftPad, y),
                                        end = Offset(leftPad + chartW, y),
                                        strokeWidth = 1f
                                    )
                                    val labelVal = (maxY * i / gridCount).toInt()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "${labelVal}s",
                                        leftPad - 6f,
                                        y + 5f,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.LTGRAY
                                            textSize = 22f
                                            textAlign = android.graphics.Paint.Align.RIGHT
                                        }
                                    )
                                }

                                val xStep = if (dates.size > 1) chartW / (dates.size - 1) else chartW / 2
                                dates.forEachIndexed { di, dateEpoch ->
                                    val x = leftPad + di * xStep
                                    if (di % maxOf(1, dates.size / 5) == 0 || di == dates.size - 1) {
                                        drawContext.canvas.nativeCanvas.drawText(
                                            LocalDate.ofEpochDay(dateEpoch).format(dateFmt),
                                            x,
                                            h - 4f,
                                            android.graphics.Paint().apply {
                                                color = android.graphics.Color.LTGRAY
                                                textSize = 20f
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }
                                        )
                                    }
                                }

                                itemIds.forEachIndexed { colorIdx, itemId ->
                                    val color = LINE_COLORS[colorIdx % LINE_COLORS.size]
                                    val itemData = dataByItem[itemId] ?: return@forEachIndexed
                                    val points = dates.mapNotNull { dateEpoch ->
                                        itemData[dateEpoch]?.let { sec ->
                                            val x = leftPad + dates.indexOf(dateEpoch) * xStep
                                            val y = 8f + chartH * (1f - sec / maxY)
                                            Offset(x, y)
                                        }
                                    }
                                    if (points.size >= 2) {
                                        for (i in 0 until points.size - 1) {
                                            drawLine(
                                                color = color,
                                                start = points[i],
                                                end = points[i + 1],
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                    points.forEach { pt ->
                                        drawCircle(color = color, radius = 5f, center = pt)
                                        drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = 5f, center = pt, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Legend
                    Text("Legend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val itemIds = state.sessionPoints.map { it.routineItemId }.distinct()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemIds.forEachIndexed { idx, itemId ->
                            val itemName = state.routineItems.firstOrNull { it.id == itemId }?.title ?: "Item $itemId"
                            val color = LINE_COLORS[idx % LINE_COLORS.size]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(itemName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
