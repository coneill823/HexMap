package com.tasktracker.data.database.dao

import androidx.room.*
import com.tasktracker.data.database.entities.RecurrenceRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceDao {
    @Query("SELECT * FROM recurrence_rules WHERE ownerId = :ownerId AND ownerType = :ownerType LIMIT 1")
    suspend fun getRule(ownerId: Long, ownerType: String): RecurrenceRule?

    @Query("SELECT * FROM recurrence_rules WHERE ownerType = :ownerType")
    fun getAllRulesForType(ownerType: String): Flow<List<RecurrenceRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurrenceRule): Long

    @Query("DELETE FROM recurrence_rules WHERE ownerId = :ownerId AND ownerType = :ownerType")
    suspend fun deleteRule(ownerId: Long, ownerType: String)
}
