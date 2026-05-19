package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.TagDao
import com.tasktracker.data.database.dao.TaskDao
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.work.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val reminderScheduler: ReminderScheduler? = null
) {
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>> = taskDao.getAllTasksWithTags()

    fun getTasksWithTagsByDate(dateEpochDay: Long): Flow<List<TaskWithTags>> =
        taskDao.getTasksWithTagsByDate(dateEpochDay)

    fun getTasksWithTagsByTagId(tagId: Long): Flow<List<TaskWithTags>> =
        taskDao.getTasksWithTagsByTagId(tagId)

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun saveTask(task: Task, tagIds: List<Long>) {
        val taskId = if (task.id == 0L) {
            taskDao.insertTask(task)
        } else {
            reminderScheduler?.cancel(task.id)
            taskDao.updateTask(task)
            task.id
        }
        taskDao.clearTaskTags(taskId)
        tagIds.forEach { tagId -> taskDao.insertTaskTag(TaskTag(taskId, tagId)) }

        val savedTask = if (task.id == 0L) task.copy(id = taskId) else task
        if (savedTask.dueDate != null && savedTask.reminderDaysBefore != null) {
            reminderScheduler?.scheduleIfNeeded(savedTask)
        }
    }

    suspend fun toggleTaskComplete(task: Task) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: Task) {
        reminderScheduler?.cancel(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun saveTag(tag: Tag): Long = tagDao.insertTag(tag)
    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
}
