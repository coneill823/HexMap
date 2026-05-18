package com.tasktracker.ui.screens.yearview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.models.DayCompletionInfo
import com.tasktracker.ui.theme.CompletedGreen
import com.tasktracker.ui.theme.EmptyDay
import com.tasktracker.ui.theme.PartialAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearViewScreen(viewModel: YearViewViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Year Overview") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                // Year navigation and overall stat
                YearHeader(
                    year = state.year,
                    completionPercent = state.overallCompletionPercent,
                    hasRoutines = state.totalRoutineItems > 0,
                    onPrevYear = { viewModel.navigateYear(-1) },
                    onNextYear = { viewModel.navigateYear(1) },
                    canGoNext = state.year < today.year
                )
            }

            item {
                if (state.totalRoutineItems == 0) {
                    NoRoutinesMessage()
                    return@item
                }

                // Month grids
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (month in 1..12) {
                        val yearMonth = YearMonth.of(state.year, month)
                        MonthStrip(
                            yearMonth = yearMonth,
                            dayCompletions = state.dayCompletions,
                            today = today
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                CompletionLegend()
            }
        }
    }
}

@Composable
private fun YearHeader(
    year: Int,
    completionPercent: Float,
    hasRoutines: Boolean,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    canGoNext: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onPrevYear) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                }
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onNextYear, enabled = canGoNext) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next year",
                        tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (hasRoutines) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Overall Completion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.1f%%".format(completionPercent),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp),
                    color = when {
                        completionPercent >= 80f -> CompletedGreen
                        completionPercent >= 50f -> PartialAmber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (completionPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        completionPercent >= 80f -> CompletedGreen
                        completionPercent >= 50f -> PartialAmber
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MonthStrip(
    yearMonth: YearMonth,
    dayCompletions: Map<Long, DayCompletionInfo>,
    today: LocalDate
) {
    val monthName = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val daysInMonth = yearMonth.lengthOfMonth()

    Column {
        Text(
            monthName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (day in 1..daysInMonth) {
                val date = yearMonth.atDay(day)
                val epochDay = date.toEpochDay()
                val completion = dayCompletions[epochDay]
                val isToday = date == today
                val isFuture = date.isAfter(today)

                val boxColor = when {
                    isFuture -> EmptyDay.copy(alpha = 0.3f)
                    completion == null -> EmptyDay
                    completion.isFullyCompleted -> CompletedGreen
                    completion.isPartiallyCompleted -> PartialAmber
                    else -> EmptyDay
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(boxColor)
                        .then(
                            if (isToday) Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            ) else Modifier
                        )
                )
            }
            // Fill remaining space to align months
            val maxDays = 31
            repeat(maxDays - daysInMonth) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompletionLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = CompletedGreen, label = "Complete")
        Spacer(Modifier.width(16.dp))
        LegendItem(color = PartialAmber, label = "Partial")
        Spacer(Modifier.width(16.dp))
        LegendItem(color = EmptyDay, label = "None")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoRoutinesMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No routines created yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create routines in the Calendar tab to track your daily progress here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
