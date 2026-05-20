package com.tasktracker.ui.screens.yearview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.models.DayCompletionInfo
import com.tasktracker.data.models.OverviewGranularity
import com.tasktracker.ui.theme.CompletedGreen
import com.tasktracker.ui.theme.EmptyDay
import com.tasktracker.ui.theme.PartialAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
                title = { Text("Overview") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Granularity selector — dropdown to avoid compressed tabs
            var granDropdownExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                ExposedDropdownMenuBox(
                    expanded = granDropdownExpanded,
                    onExpandedChange = { granDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.granularity.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = granDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = granDropdownExpanded,
                        onDismissRequest = { granDropdownExpanded = false }
                    ) {
                        OverviewGranularity.entries.forEach { gran ->
                            DropdownMenuItem(
                                text = { Text(gran.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.setGranularity(gran)
                                    granDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    // Period header with completion stat and date range
                    PeriodHeader(
                        state = state,
                        today = today,
                        onPrev = {
                            when (state.granularity) {
                                OverviewGranularity.YEAR -> viewModel.navigateYear(-1)
                                OverviewGranularity.MONTH -> viewModel.navigateMonth(-1)
                                OverviewGranularity.WEEK -> viewModel.navigateWeek(-1)
                            }
                        },
                        onNext = {
                            when (state.granularity) {
                                OverviewGranularity.YEAR -> viewModel.navigateYear(1)
                                OverviewGranularity.MONTH -> viewModel.navigateMonth(1)
                                OverviewGranularity.WEEK -> viewModel.navigateWeek(1)
                            }
                        }
                    )
                }

                // Routine filter chips
                if (state.allRoutines.isNotEmpty()) {
                    item {
                        FilterChipRow(
                            label = "Routine",
                            allItems = state.allRoutines.map { it.id to it.name },
                            selectedId = state.selectedRoutineId,
                            onSelect = { viewModel.selectRoutine(it) }
                        )
                    }
                }

                // Tag filter chips
                if (state.allTags.isNotEmpty()) {
                    item {
                        FilterChipRow(
                            label = "Tag",
                            allItems = state.allTags.map { it.id to it.name },
                            selectedId = state.selectedTagId,
                            onSelect = { viewModel.selectTag(it) }
                        )
                    }
                }

                item {
                    if (state.totalRoutineItems == 0) {
                        NoRoutinesMessage()
                        return@item
                    }

                    when (state.granularity) {
                        OverviewGranularity.YEAR -> YearGrid(
                            year = state.year,
                            dayCompletions = state.dayCompletions,
                            today = today,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OverviewGranularity.MONTH -> MonthDetailGrid(
                            yearMonth = state.month,
                            dayCompletions = state.dayCompletions,
                            today = today,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OverviewGranularity.WEEK -> WeekDetailView(
                            weekStart = state.weekStart,
                            dayCompletions = state.dayCompletions,
                            today = today,
                            totalItems = state.totalRoutineItems,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    CompletionLegend()
                }
            }
        }
    }
}

@Composable
private fun PeriodHeader(
    state: YearViewUiState,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val title = when (state.granularity) {
        OverviewGranularity.YEAR -> state.year.toString()
        OverviewGranularity.MONTH -> state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        OverviewGranularity.WEEK -> {
            val weekEnd = state.weekStart.plusDays(6)
            "${state.weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        }
    }

    val canGoNext = when (state.granularity) {
        OverviewGranularity.YEAR -> state.year < today.year
        OverviewGranularity.MONTH -> state.month.isBefore(YearMonth.now())
        OverviewGranularity.WEEK -> state.weekStart.plusWeeks(1).isBefore(today) || state.weekStart.plusWeeks(1) == today
    }

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
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (state.totalRoutineItems > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.1f%%".format(state.overallCompletionPercent),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp),
                    color = when {
                        state.overallCompletionPercent >= 80f -> CompletedGreen
                        state.overallCompletionPercent >= 50f -> PartialAmber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (state.overallCompletionPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        state.overallCompletionPercent >= 80f -> CompletedGreen
                        state.overallCompletionPercent >= 50f -> PartialAmber
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(6.dp))
                val dateRangeText = when (state.granularity) {
                    OverviewGranularity.YEAR -> {
                        val fmt = DateTimeFormatter.ofPattern("MMM d")
                        val start = LocalDate.of(state.year, 1, 1)
                        val end = LocalDate.of(state.year, 12, 31)
                        "${start.format(fmt)} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                    }
                    OverviewGranularity.MONTH -> {
                        val fmt = DateTimeFormatter.ofPattern("MMM d")
                        val start = state.month.atDay(1)
                        val end = state.month.atEndOfMonth()
                        "${start.format(fmt)} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                    }
                    OverviewGranularity.WEEK -> {
                        val fmt = DateTimeFormatter.ofPattern("MMM d")
                        val end = state.weekStart.plusDays(6)
                        "${state.weekStart.format(fmt)} – ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                    }
                }
                Text(
                    dateRangeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    label: String,
    allItems: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                    label = { Text("All") },
                    leadingIcon = if (selectedId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
            items(allItems) { (id, name) ->
                val selected = selectedId == id
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(if (selected) null else id) },
                    label = { Text(name) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun YearGrid(
    year: Int,
    dayCompletions: Map<Long, DayCompletionInfo>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (month in 1..12) {
            val yearMonth = YearMonth.of(year, month)
            MonthStrip(yearMonth = yearMonth, dayCompletions = dayCompletions, today = today)
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
        Text(monthName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            for (day in 1..daysInMonth) {
                val date = yearMonth.atDay(day)
                val epochDay = date.toEpochDay()
                val completion = dayCompletions[epochDay]
                val isFuture = date.isAfter(today)
                val isToday = date == today

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
                        .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)) else Modifier)
                )
            }
            repeat(31 - daysInMonth) { Box(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MonthDetailGrid(
    yearMonth: YearMonth,
    dayCompletions: Map<Long, DayCompletionInfo>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        val firstDay = yearMonth.atDay(1)
        val firstDayOffset = (firstDay.dayOfWeek.value - 1) % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val cells = firstDayOffset + daysInMonth
        val rows = (cells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - firstDayOffset + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum)
                            val epochDay = date.toEpochDay()
                            val completion = dayCompletions[epochDay]
                            val isFuture = date.isAfter(today)
                            val isToday = date == today

                            val boxColor = when {
                                isFuture -> EmptyDay.copy(alpha = 0.3f)
                                completion == null -> EmptyDay
                                completion.isFullyCompleted -> CompletedGreen
                                completion.isPartiallyCompleted -> PartialAmber
                                else -> EmptyDay
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(boxColor)
                                    .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (completion?.isFullyCompleted == true) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDetailView(
    weekStart: LocalDate,
    dayCompletions: Map<Long, DayCompletionInfo>,
    today: LocalDate,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val epochDay = date.toEpochDay()
            val completion = dayCompletions[epochDay]
            val isFuture = date.isAfter(today)
            val isToday = date == today

            val bgColor = when {
                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                completion == null -> MaterialTheme.colorScheme.surfaceVariant
                completion.isFullyCompleted -> CompletedGreen.copy(alpha = 0.2f)
                completion.isPartiallyCompleted -> PartialAmber.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            date.format(DateTimeFormatter.ofPattern("MMM d")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isFuture && totalItems > 0) {
                        val completed = completion?.completedCount ?: 0
                        Text(
                            "$completed / $totalItems",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                completion?.isFullyCompleted == true -> CompletedGreen
                                completed > 0 -> PartialAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoRoutinesMessage() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No routines created yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Create routines in the Calendar tab to track your daily progress here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}
