package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.TaskSessionLog
import kotlinx.coroutines.flow.Flow

data class TaskAvgTime(val taskId: Long, val avgSeconds: Float)

@Dao
interface TaskSessionLogDao {
    @Insert
    suspend fun insertLog(log: TaskSessionLog)

    @Insert
    suspend fun insertLogs(logs: List<TaskSessionLog>)

    @Query("SELECT taskId, AVG(elapsedSeconds) as avgSeconds FROM task_session_logs WHERE taskId = :taskId GROUP BY taskId")
    suspend fun getAverageTimeForTask(taskId: Long): TaskAvgTime?

    @Query("SELECT * FROM task_session_logs WHERE taskId = :taskId ORDER BY dateEpochDay ASC")
    fun getLogsForTask(taskId: Long): Flow<List<TaskSessionLog>>
}
