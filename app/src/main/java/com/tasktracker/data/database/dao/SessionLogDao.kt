package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.RoutineSessionLog
import kotlinx.coroutines.flow.Flow

data class ItemAvgSeconds(val routineItemId: Long, val avgSeconds: Float)

@Dao
interface SessionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RoutineSessionLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<RoutineSessionLog>)

    @Query("SELECT routineItemId, AVG(elapsedSeconds) as avgSeconds FROM routine_session_logs WHERE routineId = :routineId GROUP BY routineItemId")
    suspend fun getAverageTimePerItem(routineId: Long): List<ItemAvgSeconds>

    @Query("SELECT * FROM routine_session_logs WHERE routineId = :routineId ORDER BY startedAt DESC")
    fun getLogsForRoutine(routineId: Long): Flow<List<RoutineSessionLog>>

    @Query("""
        SELECT routineItemId, dateEpochDay, AVG(elapsedSeconds) as avgSeconds
        FROM routine_session_logs
        WHERE routineId = :routineId
        GROUP BY routineItemId, dateEpochDay
        ORDER BY dateEpochDay ASC
    """)
    fun getSessionPointsForRoutine(routineId: Long): Flow<List<ItemSessionPoint>>
}

data class ItemSessionPoint(val routineItemId: Long, val dateEpochDay: Long, val avgSeconds: Float)
