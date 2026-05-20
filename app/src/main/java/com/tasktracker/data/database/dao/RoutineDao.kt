package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.DailyRoutineCompletion
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.RoutineTag
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.models.RoutineWithItems
import kotlinx.coroutines.flow.Flow

data class DateCompletedCount(
    val dateEpochDay: Long,
    val completed: Int
)

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllRoutinesWithItems(): Flow<List<RoutineWithItems>>

    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("UPDATE routines SET deletedAt = :timestamp WHERE id = :routineId")
    suspend fun softDelete(routineId: Long, timestamp: Long)

    @Query("SELECT * FROM routines WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedRoutines(): Flow<List<Routine>>

    @Query("UPDATE routines SET deletedAt = NULL WHERE id = :routineId")
    suspend fun restoreRoutine(routineId: Long)

    @Query("DELETE FROM routines WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeExpiredRoutines(cutoff: Long)

    @Query("SELECT * FROM routine_items WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getItemsForRoutine(routineId: Long): Flow<List<RoutineItem>>

    @Query("SELECT * FROM daily_routine_completions WHERE dateEpochDay = :dateEpochDay")
    fun getCompletionsForDate(dateEpochDay: Long): Flow<List<DailyRoutineCompletion>>

    @Query("SELECT COUNT(*) FROM routine_items")
    suspend fun getTotalItemCount(): Int

    @Query("""
        SELECT dateEpochDay, COUNT(*) as completed
        FROM daily_routine_completions
        WHERE isCompleted = 1 AND dateEpochDay >= :startDate AND dateEpochDay <= :endDate
        GROUP BY dateEpochDay
    """)
    fun getCompletedCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<DateCompletedCount>>

    @Query("""
        SELECT dateEpochDay, COUNT(*) as completed
        FROM daily_routine_completions
        WHERE isCompleted = 1 AND routineId = :routineId
          AND dateEpochDay >= :startDate AND dateEpochDay <= :endDate
        GROUP BY dateEpochDay
    """)
    fun getCompletedCountsByDateRangeAndRoutine(
        routineId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<DateCompletedCount>>

    @Query("SELECT COUNT(*) FROM routine_items WHERE routineId = :routineId")
    suspend fun getItemCountForRoutine(routineId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineItem(item: RoutineItem): Long

    @Update
    suspend fun updateRoutineItem(item: RoutineItem)

    @Delete
    suspend fun deleteRoutineItem(item: RoutineItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: DailyRoutineCompletion)

    @Query("UPDATE routine_items SET orderIndex = :newIndex WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: Long, newIndex: Int)

    @Query("SELECT * FROM routine_items WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getItemsForRoutineOnce(routineId: Long): List<RoutineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTag(tag: RoutineTag)

    @Query("DELETE FROM routine_tags WHERE routineId = :routineId")
    suspend fun clearRoutineTags(routineId: Long)

    @Query("SELECT tags.* FROM tags INNER JOIN routine_tags ON tags.id = routine_tags.tagId WHERE routine_tags.routineId = :routineId")
    fun getTagsForRoutine(routineId: Long): Flow<List<Tag>>
}
