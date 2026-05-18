package com.tasktracker.data.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.database.entities.TaskTag

data class TaskWithTags(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTag::class,
            parentColumn = "taskId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)

data class RoutineWithItems(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val items: List<RoutineItem>
)
