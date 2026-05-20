package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.RecurrenceDao
import com.tasktracker.data.database.entities.RecurrenceRule
import kotlinx.coroutines.flow.Flow

class RecurrenceRepository(private val recurrenceDao: RecurrenceDao) {
    suspend fun getRule(ownerId: Long, ownerType: String): RecurrenceRule? =
        recurrenceDao.getRule(ownerId, ownerType)

    fun getAllRulesForType(ownerType: String): Flow<List<RecurrenceRule>> =
        recurrenceDao.getAllRulesForType(ownerType)

    fun getAllRules(): Flow<List<RecurrenceRule>> = recurrenceDao.getAllRules()

    suspend fun saveRule(rule: RecurrenceRule) {
        recurrenceDao.deleteRule(rule.ownerId, rule.ownerType)
        recurrenceDao.insertRule(rule)
    }

    suspend fun deleteRule(ownerId: Long, ownerType: String) =
        recurrenceDao.deleteRule(ownerId, ownerType)
}
