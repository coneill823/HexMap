package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.TaskAvgTime
import com.tasktracker.data.database.dao.TaskSessionLogDao
import com.tasktracker.data.database.entities.TaskSessionLog
import kotlinx.coroutines.flow.Flow

class TaskSessionLogRepository(private val taskSessionLogDao: TaskSessionLogDao) {
    suspend fun insertLog(log: TaskSessionLog) = taskSessionLogDao.insertLog(log)

    suspend fun insertLogs(logs: List<TaskSessionLog>) = taskSessionLogDao.insertLogs(logs)

    suspend fun getAverageTimeForTask(taskId: Long): TaskAvgTime? =
        taskSessionLogDao.getAverageTimeForTask(taskId)

    fun getLogsForTask(taskId: Long): Flow<List<TaskSessionLog>> =
        taskSessionLogDao.getLogsForTask(taskId)
}
