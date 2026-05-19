package com.tasktracker.work

import android.content.Context
import androidx.work.*
import com.tasktracker.data.database.entities.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun scheduleIfNeeded(task: Task) {
        val dueDate = task.dueDate ?: return
        val minutesBefore = task.reminderDaysBefore ?: return  // field stores minutes
        if (minutesBefore <= 0) return

        val dueLocalDate = LocalDate.ofEpochDay(dueDate)
        val dueTime = if (task.timeMinutes != null) {
            LocalDateTime.of(dueLocalDate, LocalTime.of(task.timeMinutes / 60, task.timeMinutes % 60))
        } else {
            LocalDateTime.of(dueLocalDate, LocalTime.of(9, 0))
        }

        val reminderTime = dueTime.minusMinutes(minutesBefore.toLong())
        val now = LocalDateTime.now()
        if (reminderTime.isBefore(now)) return

        val delayMillis = java.time.Duration.between(now, reminderTime).toMillis()

        val humanLabel = when {
            minutesBefore < 60 -> "$minutesBefore min before"
            minutesBefore == 60 -> "1 hour before"
            minutesBefore < 1440 -> "${minutesBefore / 60} hours before"
            minutesBefore == 1440 -> "1 day before"
            else -> "${minutesBefore / 1440} days before"
        }

        val data = workDataOf(
            ReminderWorker.KEY_TASK_ID to task.id,
            ReminderWorker.KEY_TASK_TITLE to task.title,
            ReminderWorker.KEY_DAYS_UNTIL_DUE to minutesBefore
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
    }

    companion object {
        val REMINDER_OPTIONS = listOf(
            5 to "5 minutes before",
            15 to "15 minutes before",
            30 to "30 minutes before",
            60 to "1 hour before",
            120 to "2 hours before",
            180 to "3 hours before",
            360 to "6 hours before",
            720 to "12 hours before",
            1440 to "1 day before",
            2880 to "2 days before",
            4320 to "3 days before",
            10080 to "1 week before"
        )
    }
}
