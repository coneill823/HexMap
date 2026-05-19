package com.tasktracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasktracker.data.database.entities.RecurrenceRule

data class RecurrenceDraft(
    val enabled: Boolean = false,
    val frequency: String = "daily",
    val interval: Int = 1,
    val dayOfWeekMask: Int? = null,
    val nthWeekday: Int? = null,
    val weekdayOfMonth: Int? = null
) {
    fun toRule(ownerId: Long, ownerType: String, startEpochDay: Long) = RecurrenceRule(
        ownerId = ownerId,
        ownerType = ownerType,
        frequency = frequency,
        interval = interval,
        dayOfWeekMask = dayOfWeekMask,
        nthWeekday = nthWeekday,
        weekdayOfMonth = weekdayOfMonth,
        startEpochDay = startEpochDay
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePickerSection(
    draft: RecurrenceDraft,
    onDraftChange: (RecurrenceDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Recurrence", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = draft.enabled,
                onCheckedChange = { onDraftChange(draft.copy(enabled = it)) }
            )
        }

        if (draft.enabled) {
            // Frequency selector
            val frequencies = listOf("daily", "weekly", "monthly", "yearly")
            val freqLabels = listOf("Daily", "Weekly", "Monthly", "Yearly")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                frequencies.forEachIndexed { i, freq ->
                    FilterChip(
                        selected = draft.frequency == freq,
                        onClick = { onDraftChange(draft.copy(frequency = freq, dayOfWeekMask = null, nthWeekday = null, weekdayOfMonth = null)) },
                        label = { Text(freqLabels[i], style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Interval
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Every", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = draft.interval.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.takeIf { it > 0 }?.let { onDraftChange(draft.copy(interval = it)) } },
                    modifier = Modifier.width(60.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                val unitLabel = when (draft.frequency) {
                    "daily" -> if (draft.interval == 1) "day" else "days"
                    "weekly" -> if (draft.interval == 1) "week" else "weeks"
                    "monthly" -> if (draft.interval == 1) "month" else "months"
                    "yearly" -> if (draft.interval == 1) "year" else "years"
                    else -> ""
                }
                Text(unitLabel, style = MaterialTheme.typography.bodySmall)
            }

            // Weekly: day of week selection
            if (draft.frequency == "weekly") {
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayNames.forEachIndexed { i, name ->
                        val bit = 1 shl i
                        val mask = draft.dayOfWeekMask ?: 0
                        val selected = mask and bit != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val newMask = if (selected) mask and bit.inv() else mask or bit
                                onDraftChange(draft.copy(dayOfWeekMask = if (newMask == 0) null else newMask))
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Monthly: Nth weekday option
            if (draft.frequency == "monthly") {
                var useNthWeekday by remember { mutableStateOf(draft.nthWeekday != null) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = useNthWeekday,
                        onCheckedChange = { use ->
                            useNthWeekday = use
                            if (!use) onDraftChange(draft.copy(nthWeekday = null, weekdayOfMonth = null))
                            else onDraftChange(draft.copy(nthWeekday = 1, weekdayOfMonth = 1))
                        }
                    )
                    Text("By weekday (e.g. 2nd Thursday)", style = MaterialTheme.typography.bodySmall)
                }
                if (useNthWeekday) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val nthLabels = listOf("1st", "2nd", "3rd", "4th", "5th")
                        nthLabels.forEachIndexed { i, lbl ->
                            FilterChip(
                                selected = draft.nthWeekday == i + 1,
                                onClick = { onDraftChange(draft.copy(nthWeekday = i + 1)) },
                                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    val dayNames2 = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dayNames2.forEachIndexed { i, name ->
                            FilterChip(
                                selected = draft.weekdayOfMonth == i + 1,
                                onClick = { onDraftChange(draft.copy(weekdayOfMonth = i + 1)) },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Summary text
            if (draft.enabled) {
                val summary = remember(draft) {
                    val temp = RecurrenceRule(
                        ownerId = 0, ownerType = "",
                        frequency = draft.frequency,
                        interval = draft.interval,
                        dayOfWeekMask = draft.dayOfWeekMask,
                        nthWeekday = draft.nthWeekday,
                        weekdayOfMonth = draft.weekdayOfMonth
                    )
                    temp.displayText()
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
