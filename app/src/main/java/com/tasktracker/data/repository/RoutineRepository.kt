package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.RoutineDao
import com.tasktracker.data.database.entities.DailyRoutineCompletion
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.models.RoutineWithItems
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {
    fun getAllRoutinesWithItems(): Flow<List<RoutineWithItems>> =
        routineDao.getAllRoutinesWithItems()

    fun getCompletionsForDate(dateEpochDay: Long) =
        routineDao.getCompletionsForDate(dateEpochDay)

    fun getCompletedCountsByDateRange(startDate: Long, endDate: Long) =
        routineDao.getCompletedCountsByDateRange(startDate, endDate)

    suspend fun getTotalItemCount(): Int = routineDao.getTotalItemCount()

    suspend fun saveRoutine(routine: Routine): Long =
        if (routine.id == 0L) routineDao.insertRoutine(routine)
        else { routineDao.updateRoutine(routine); routine.id }

    suspend fun deleteRoutine(routine: Routine) = routineDao.deleteRoutine(routine)

    suspend fun saveRoutineItem(item: RoutineItem): Long =
        if (item.id == 0L) routineDao.insertRoutineItem(item)
        else { routineDao.updateRoutineItem(item); item.id }

    suspend fun deleteRoutineItem(item: RoutineItem) = routineDao.deleteRoutineItem(item)

    suspend fun setItemCompletion(
        routineId: Long,
        routineItemId: Long,
        dateEpochDay: Long,
        isCompleted: Boolean
    ) {
        routineDao.upsertCompletion(
            DailyRoutineCompletion(routineId, routineItemId, dateEpochDay, isCompleted)
        )
    }

    suspend fun reorderItems(routineId: Long, fromIndex: Int, toIndex: Int) {
        val items = routineDao.getItemsForRoutineOnce(routineId).toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
        items.forEachIndexed { index, routineItem ->
            routineDao.updateItemOrder(routineItem.id, index)
        }
    }

    suspend fun updateRoutineItem(item: RoutineItem) = routineDao.updateRoutineItem(item)
}
