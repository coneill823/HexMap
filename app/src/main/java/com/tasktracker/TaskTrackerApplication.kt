package com.tasktracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.tasktracker.data.database.AppDatabase
import com.tasktracker.data.repository.RecurrenceRepository
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.SessionLogRepository
import com.tasktracker.data.repository.TaskRepository
import com.tasktracker.work.ReminderScheduler
import com.tasktracker.work.ReminderWorker

class TaskTrackerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
    val taskRepository by lazy { TaskRepository(database.taskDao(), database.tagDao(), reminderScheduler) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
    val recurrenceRepository by lazy { RecurrenceRepository(database.recurrenceDao()) }
    val sessionLogRepository by lazy { SessionLogRepository(database.sessionLogDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for upcoming task due dates"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
