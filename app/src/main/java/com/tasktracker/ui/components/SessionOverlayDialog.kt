package com.tasktracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tasktracker.data.models.SessionItemResult
import com.tasktracker.data.models.SessionState

@Composable
fun SessionOverlayDialog(
    state: SessionState,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isFinished) {
                SessionResultsScreen(
                    completedItems = state.completedItems,
                    averages = state.averages,
                    onDismiss = onDismiss
                )
            } else {
                val items = state.routine.items
                val currentItem = items.getOrNull(state.currentItemIndex)?.item
                val isLastItem = state.currentItemIndex >= items.size - 1

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        state.routine.routine.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Task ${state.currentItemIndex + 1} of ${items.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { state.currentItemIndex.toFloat() / items.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(Modifier.height(16.dp))

                    // Current task card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                currentItem?.title ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentItem?.description?.isNotBlank() == true) {
                                Text(
                                    currentItem.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Timer display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer, contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formatElapsed(state.timerSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Completed items mini list
                    if (state.completedItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.completedItems.forEach { result ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        result.item.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Text(
                                        formatElapsed(result.elapsedSeconds),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = if (isLastItem) onFinish else onNext,
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(if (isLastItem) "Finish" else "Next Task")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionResultsScreen(
    completedItems: List<SessionItemResult>,
    averages: Map<Long, Float>,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val avgDotColor = MaterialTheme.colorScheme.tertiary
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Session Complete!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Time breakdown", style = MaterialTheme.typography.titleMedium)

        if (completedItems.isNotEmpty()) {
            val maxSeconds = completedItems.maxOf { it.elapsedSeconds }.coerceAtLeast(1)
            val barHeight = 28.dp
            val labelWidth = 100.dp
            val chartHeight = barHeight * completedItems.size + 8.dp * (completedItems.size - 1)
            val labelWidthPx = 100f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 32.dp)
            ) {
                val availableWidth = size.width - labelWidthPx - 16f
                val rowHeight = barHeight.toPx()
                val spacing = 8.dp.toPx()

                completedItems.forEachIndexed { index, result ->
                    val y = index * (rowHeight + spacing)
                    val barWidth = (result.elapsedSeconds.toFloat() / maxSeconds) * availableWidth
                    val avgWidth = averages[result.item.id]?.let { avg ->
                        (avg / maxSeconds) * availableWidth
                    }

                    // Label
                    val textLayout = textMeasurer.measure(
                        text = result.item.title.take(14),
                        style = TextStyle(fontSize = 11.sp, color = onSurfaceVariantColor)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(0f, y + (rowHeight - textLayout.size.height) / 2f)
                    )

                    // Bar
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(labelWidthPx, y),
                        size = Size(barWidth.coerceAtLeast(4f), rowHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )

                    // Average dot
                    avgWidth?.let { aw ->
                        drawCircle(
                            color = avgDotColor,
                            radius = 6.dp.toPx(),
                            center = Offset(labelWidthPx + aw, y + rowHeight / 2f)
                        )
                    }

                    // Time label
                    val timeLayout = textMeasurer.measure(
                        text = formatElapsed(result.elapsedSeconds),
                        style = TextStyle(fontSize = 10.sp, color = onSurfaceColor)
                    )
                    drawText(
                        textLayoutResult = timeLayout,
                        topLeft = Offset(
                            labelWidthPx + barWidth.coerceAtLeast(4f) + 4f,
                            y + (rowHeight - timeLayout.size.height) / 2f
                        )
                    )
                }
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp, 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text("This session", style = MaterialTheme.typography.labelSmall)
                }
                if (averages.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                        Text("Historical avg", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        val totalSeconds = completedItems.sumOf { it.elapsedSeconds }
        Text(
            "Total time: ${formatElapsed(totalSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

private fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
