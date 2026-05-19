package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.DailyRoutineCompletion
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.models.RoutineWithItems
import kotlinx.coroutines.flow.Flow

data class DateCompletedCount(
    val dateEpochDay: Long,
    val completed: Int
)

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutinesWithItems(): Flow<List<RoutineWithItems>>

    @Query("SELECT * FROM routines ORDER BY createdAt ASC")
    fun getAllRoutines(): Flow<List<Routine>>

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
}
