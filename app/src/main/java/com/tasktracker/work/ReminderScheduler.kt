package com.tasktracker.work

import android.content.Context
import androidx.work.*
import com.tasktracker.data.database.entities.Task
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun scheduleIfNeeded(task: Task) {
        val dueDate = task.dueDate ?: return
        val daysBefore = task.reminderDaysBefore ?: return
        if (daysBefore < 0) return

        val reminderEpochDay = dueDate - daysBefore
        val today = LocalDate.now().toEpochDay()
        val delayDays = reminderEpochDay - today
        if (delayDays < 0) return

        val delayMillis = delayDays * 24 * 60 * 60 * 1000L

        val data = workDataOf(
            ReminderWorker.KEY_TASK_ID to task.id,
            ReminderWorker.KEY_TASK_TITLE to task.title,
            ReminderWorker.KEY_DAYS_UNTIL_DUE to daysBefore
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
}
