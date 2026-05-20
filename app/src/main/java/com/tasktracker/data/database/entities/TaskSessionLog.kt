package com.tasktracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_session_logs")
data class TaskSessionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val sessionId: String,
    val elapsedSeconds: Int,
    val dateEpochDay: Long
)
