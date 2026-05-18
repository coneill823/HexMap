package com.tasktracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tasktracker.data.database.dao.RoutineDao
import com.tasktracker.data.database.dao.TagDao
import com.tasktracker.data.database.dao.TaskDao
import com.tasktracker.data.database.entities.DailyRoutineCompletion
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag

@Database(
    entities = [
        Tag::class,
        Task::class,
        TaskTag::class,
        Routine::class,
        RoutineItem::class,
        DailyRoutineCompletion::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_tracker_db"
                ).build().also { INSTANCE = it }
            }
    }
}
