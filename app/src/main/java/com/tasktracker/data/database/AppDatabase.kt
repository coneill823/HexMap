package com.tasktracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tasktracker.data.database.dao.RecurrenceDao
import com.tasktracker.data.database.dao.RoutineDao
import com.tasktracker.data.database.dao.SessionLogDao
import com.tasktracker.data.database.dao.TagDao
import com.tasktracker.data.database.dao.TaskDao
import com.tasktracker.data.database.entities.DailyRoutineCompletion
import com.tasktracker.data.database.entities.RecurrenceRule
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.RoutineSessionLog
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN dueDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderDaysBefore INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderWorkerId TEXT")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recurrence_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ownerId INTEGER NOT NULL,
                ownerType TEXT NOT NULL,
                frequency TEXT NOT NULL,
                `interval` INTEGER NOT NULL DEFAULT 1,
                dayOfWeekMask INTEGER,
                nthWeekday INTEGER,
                weekdayOfMonth INTEGER,
                startEpochDay INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrence_rules_ownerId ON recurrence_rules (ownerId)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS routine_session_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER NOT NULL,
                routineItemId INTEGER NOT NULL,
                sessionId TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                elapsedSeconds INTEGER NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                FOREIGN KEY (routineId) REFERENCES routines(id) ON DELETE CASCADE,
                FOREIGN KEY (routineItemId) REFERENCES routine_items(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_session_logs_routineId ON routine_session_logs (routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_session_logs_routineItemId ON routine_session_logs (routineItemId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routines ADD COLUMN timeMinutes INTEGER")
    }
}

@Database(
    entities = [
        Tag::class,
        Task::class,
        TaskTag::class,
        Routine::class,
        RoutineItem::class,
        DailyRoutineCompletion::class,
        RecurrenceRule::class,
        RoutineSessionLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun sessionLogDao(): SessionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
