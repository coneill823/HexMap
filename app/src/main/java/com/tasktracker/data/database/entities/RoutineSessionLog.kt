package com.tasktracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_session_logs",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineItem::class,
            parentColumns = ["id"],
            childColumns = ["routineItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("routineItemId")]
)
data class RoutineSessionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val routineItemId: Long,
    val sessionId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val elapsedSeconds: Int,
    val dateEpochDay: Long
)
