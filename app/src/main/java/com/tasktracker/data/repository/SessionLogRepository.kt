package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.ItemAvgSeconds
import com.tasktracker.data.database.dao.SessionLogDao
import com.tasktracker.data.database.entities.RoutineSessionLog
import kotlinx.coroutines.flow.Flow

class SessionLogRepository(private val sessionLogDao: SessionLogDao) {
    suspend fun saveLogs(logs: List<RoutineSessionLog>) = sessionLogDao.insertLogs(logs)

    suspend fun getAverageTimePerItem(routineId: Long): List<ItemAvgSeconds> =
        sessionLogDao.getAverageTimePerItem(routineId)

    fun getLogsForRoutine(routineId: Long): Flow<List<RoutineSessionLog>> =
        sessionLogDao.getLogsForRoutine(routineId)
}
