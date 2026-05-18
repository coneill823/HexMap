package com.tasktracker

import android.app.Application
import com.tasktracker.data.database.AppDatabase
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.TaskRepository

class TaskTrackerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val taskRepository by lazy { TaskRepository(database.taskDao(), database.tagDao()) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
}
