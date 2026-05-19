package com.tasktracker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tasktracker.TaskTrackerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as TaskTrackerApplication
        val scheduler = ReminderScheduler(context)
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = app.taskRepository.getAllTasksWithTags().first()
            tasks.forEach { twt ->
                val task = twt.task
                if (task.dueDate != null && task.reminderDaysBefore != null) {
                    scheduler.scheduleIfNeeded(task)
                }
            }
        }
    }
}
