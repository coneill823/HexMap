package com.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            Icon(
                Icons.Default.Repeat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Recurrence", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = draft.enabled,
                onCheckedChange = { onDraftChange(draft.copy(enabled = it)) }
            )
        }

        if (draft.enabled) {
            // ── Frequency dropdown ──────────────────────────────────────────
            val frequencies = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly")
            var freqExpanded by remember { mutableStateOf(false) }
            val selectedFreqLabel = frequencies.first { it.first == draft.frequency }.second

            ExposedDropdownMenuBox(
                expanded = freqExpanded,
                onExpandedChange = { freqExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedFreqLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = freqExpanded,
                    onDismissRequest = { freqExpanded = false }
                ) {
                    frequencies.forEach { (freq, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onDraftChange(draft.copy(
                                    frequency = freq,
                                    dayOfWeekMask = null,
                                    nthWeekday = null,
                                    weekdayOfMonth = null
                                ))
                                freqExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Interval ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Every", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = draft.interval.toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.takeIf { it > 0 }?.let {
                            onDraftChange(draft.copy(interval = it))
                        }
                    },
                    modifier = Modifier.width(72.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                val unitLabel = when (draft.frequency) {
                    "daily"   -> if (draft.interval == 1) "day" else "days"
                    "weekly"  -> if (draft.interval == 1) "week" else "weeks"
                    "monthly" -> if (draft.interval == 1) "month" else "months"
                    "yearly"  -> if (draft.interval == 1) "year" else "years"
                    else -> ""
                }
                Text(unitLabel, style = MaterialTheme.typography.bodySmall)
            }

            // ── Weekly: day-of-week circle buttons ────────────────────────
            if (draft.frequency == "weekly") {
                Text("Repeat on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // First letters: M T W T F S S (Mon..Sun, bits 1,2,4,8,16,32,64)
                val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLetters.forEachIndexed { i, letter ->
                        val bit = 1 shl i
                        val mask = draft.dayOfWeekMask ?: 0
                        val selected = mask and bit != 0
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    val newMask = if (selected) mask and bit.inv() else mask or bit
                                    onDraftChange(draft.copy(dayOfWeekMask = if (newMask == 0) null else newMask))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Monthly: Nth weekday ───────────────────────────────────────
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
                    // "Occurrence" dropdown (1st – 5th)
                    val nthOptions = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 5 to "5th")
                    var nthExpanded by remember { mutableStateOf(false) }
                    val currentNth = nthOptions.firstOrNull { it.first == draft.nthWeekday }?.second ?: "1st"

                    ExposedDropdownMenuBox(
                        expanded = nthExpanded,
                        onExpandedChange = { nthExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentNth,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Occurrence") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nthExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = nthExpanded,
                            onDismissRequest = { nthExpanded = false }
                        ) {
                            nthOptions.forEach { (n, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onDraftChange(draft.copy(nthWeekday = n))
                                        nthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // "Day of week" dropdown (Mon – Sun)
                    val dayOptions = listOf(
                        1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
                        4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday"
                    )
                    var dayExpanded by remember { mutableStateOf(false) }
                    val currentDay = dayOptions.firstOrNull { it.first == draft.weekdayOfMonth }?.second ?: "Monday"

                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentDay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of week") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            dayOptions.forEach { (d, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onDraftChange(draft.copy(weekdayOfMonth = d))
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Summary text ───────────────────────────────────────────────
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
