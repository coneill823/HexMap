package com.tasktracker.data.models

import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.Tag

enum class OverviewGranularity { YEAR, MONTH, WEEK }

data class SessionItemResult(
    val item: RoutineItem,
    val elapsedSeconds: Int
)

data class SessionState(
    val routine: RoutineWithProgress,
    val currentItemIndex: Int = 0,
    val timerSeconds: Int = 0,
    val completedItems: List<SessionItemResult> = emptyList(),
    val isFinished: Boolean = false,
    val averages: Map<Long, Float> = emptyMap()
)

data class RoutineItemWithCompletion(
    val item: RoutineItem,
    val isCompleted: Boolean
)

data class RoutineWithProgress(
    val routine: Routine,
    val items: List<RoutineItemWithCompletion>,
    val tags: List<Tag> = emptyList()
) {
    val completedCount: Int get() = items.count { it.isCompleted }
    val totalCount: Int get() = items.size
    val isFullyCompleted: Boolean get() = items.isNotEmpty() && items.all { it.isCompleted }
    val progress: Float get() = if (items.isEmpty()) 0f else completedCount.toFloat() / totalCount
}

data class DayCompletionInfo(
    val dateEpochDay: Long,
    val completedCount: Int,
    val totalCount: Int
) {
    val isFullyCompleted: Boolean get() = totalCount > 0 && completedCount >= totalCount
    val isPartiallyCompleted: Boolean get() = completedCount > 0 && completedCount < totalCount
    val progress: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}
