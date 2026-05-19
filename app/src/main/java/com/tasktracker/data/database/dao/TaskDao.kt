package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag
import com.tasktracker.data.models.TaskWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY scheduledDate ASC, timeMinutes ASC, id ASC")
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE scheduledDate = :dateEpochDay ORDER BY timeMinutes ASC, id ASC")
    fun getTasksWithTagsByDate(dateEpochDay: Long): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT tasks.* FROM tasks INNER JOIN task_tags ON tasks.id = task_tags.taskId WHERE task_tags.tagId = :tagId ORDER BY tasks.scheduledDate ASC, tasks.timeMinutes ASC")
    fun getTasksWithTagsByTagId(tagId: Long): Flow<List<TaskWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTag(taskTag: TaskTag)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearTaskTags(taskId: Long)

    @Query("""
        SELECT tasks.scheduledDate as dateEpochDay, COUNT(*) as completed
        FROM tasks INNER JOIN task_tags ON tasks.id = task_tags.taskId
        WHERE tasks.isCompleted = 1 AND task_tags.tagId = :tagId
          AND tasks.scheduledDate >= :startDate AND tasks.scheduledDate <= :endDate
        GROUP BY tasks.scheduledDate
    """)
    fun getCompletedTaskCountsByTagAndDateRange(
        tagId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<com.tasktracker.data.database.dao.DateCompletedCount>>

    @Query("""
        SELECT COUNT(DISTINCT tasks.id) FROM tasks
        INNER JOIN task_tags ON tasks.id = task_tags.taskId
        WHERE task_tags.tagId = :tagId
          AND tasks.scheduledDate >= :startDate AND tasks.scheduledDate <= :endDate
    """)
    suspend fun getTaskCountByTagAndDateRange(tagId: Long, startDate: Long, endDate: Long): Int
}
