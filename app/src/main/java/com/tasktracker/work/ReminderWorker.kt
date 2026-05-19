package com.tasktracker.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tasktracker.MainActivity

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val daysUntilDue = inputData.getInt(KEY_DAYS_UNTIL_DUE, 0)

        val message = when (daysUntilDue) {
            0 -> "\"$title\" is due today!"
            1 -> "\"$title\" is due tomorrow"
            else -> "\"$title\" is due in $daysUntilDue days"
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(taskId.toInt(), notification)

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_DAYS_UNTIL_DUE = "days_until_due"
    }
}
