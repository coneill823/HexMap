package com.tasktracker.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Entity(tableName = "recurrence_rules", indices = [Index("ownerId")])
data class RecurrenceRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: Long,
    val ownerType: String, // "task" or "routine"
    val frequency: String, // "daily", "weekly", "monthly", "yearly"
    val interval: Int = 1,
    // Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val dayOfWeekMask: Int? = null,
    // For "Nth weekday of month": which occurrence (1-5)
    val nthWeekday: Int? = null,
    // For "Nth weekday of month": which day (1=Mon..7=Sun)
    val weekdayOfMonth: Int? = null,
    val startEpochDay: Long = 0L
) {
    fun occursOn(date: LocalDate): Boolean {
        val start = LocalDate.ofEpochDay(startEpochDay)
        if (date.isBefore(start)) return false
        return when (frequency) {
            "daily" -> {
                val daysBetween = date.toEpochDay() - start.toEpochDay()
                daysBetween % interval == 0L
            }
            "weekly" -> {
                val mask = dayOfWeekMask ?: (1 shl (start.dayOfWeek.value - 1))
                val weeksBetween = (date.toEpochDay() - start.toEpochDay()) / 7
                if (weeksBetween % interval != 0L) return false
                val dayBit = 1 shl (date.dayOfWeek.value - 1)
                mask and dayBit != 0
            }
            "monthly" -> {
                val monthsBetween = (date.year - start.year) * 12 + (date.monthValue - start.monthValue)
                if (monthsBetween < 0 || monthsBetween % interval != 0) return false
                if (nthWeekday != null && weekdayOfMonth != null) {
                    val dow = DayOfWeek.of(weekdayOfMonth)
                    val nthDay = date.with(TemporalAdjusters.dayOfWeekInMonth(nthWeekday, dow))
                    date == nthDay
                } else {
                    date.dayOfMonth == start.dayOfMonth
                }
            }
            "yearly" -> {
                val yearsBetween = date.year - start.year
                yearsBetween >= 0 && yearsBetween % interval == 0 &&
                    date.monthValue == start.monthValue && date.dayOfMonth == start.dayOfMonth
            }
            else -> false
        }
    }

    fun displayText(): String = when (frequency) {
        "daily" -> if (interval == 1) "Every day" else "Every $interval days"
        "weekly" -> {
            val days = buildList {
                if (dayOfWeekMask != null) {
                    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    names.forEachIndexed { i, name -> if (dayOfWeekMask and (1 shl i) != 0) add(name) }
                }
            }
            val dayStr = if (days.isEmpty()) "" else " on ${days.joinToString(", ")}"
            if (interval == 1) "Every week$dayStr" else "Every $interval weeks$dayStr"
        }
        "monthly" -> {
            if (nthWeekday != null && weekdayOfMonth != null) {
                val nth = listOf("", "1st", "2nd", "3rd", "4th", "5th")[nthWeekday]
                val dayName = DayOfWeek.of(weekdayOfMonth).name.lowercase().replaceFirstChar { it.uppercase() }
                if (interval == 1) "Every month on the $nth $dayName"
                else "Every $interval months on the $nth $dayName"
            } else {
                if (interval == 1) "Every month" else "Every $interval months"
            }
        }
        "yearly" -> if (interval == 1) "Every year" else "Every $interval years"
        else -> frequency
    }
}
