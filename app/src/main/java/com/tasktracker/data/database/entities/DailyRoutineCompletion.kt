package com.tasktracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "daily_routine_completions",
    primaryKeys = ["routineId", "routineItemId", "dateEpochDay"],
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
data class DailyRoutineCompletion(
    val routineId: Long,
    val routineItemId: Long,
    val dateEpochDay: Long,
    val isCompleted: Boolean = false
)
