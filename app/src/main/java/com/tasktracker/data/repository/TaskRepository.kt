package com.tasktracker.data.repository

import com.tasktracker.data.database.dao.TagDao
import com.tasktracker.data.database.dao.TaskDao
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag
import com.tasktracker.data.models.TaskWithTags
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao
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
            taskDao.updateTask(task)
            task.id
        }
        taskDao.clearTaskTags(taskId)
        tagIds.forEach { tagId -> taskDao.insertTaskTag(TaskTag(taskId, tagId)) }
    }

    suspend fun toggleTaskComplete(task: Task) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun saveTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
}
