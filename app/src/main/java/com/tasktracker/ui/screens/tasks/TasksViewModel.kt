package com.tasktracker.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.models.RoutineItemWithCompletion
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.SessionItemResult
import com.tasktracker.data.models.SessionState
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.SessionLogRepository
import com.tasktracker.data.repository.TaskRepository
import com.tasktracker.data.database.entities.RoutineSessionLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class TaskPlayState(
    val task: Task,
    val timerSeconds: Int = 0,
    val isFinished: Boolean = false
)

data class TasksUiState(
    val tasks: List<TaskWithTags> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val routines: List<RoutineWithProgress> = emptyList(),
    val selectedTagId: Long? = null,
    val showAddTaskDialog: Boolean = false,
    val showManageTagsDialog: Boolean = false,
    val editingTask: TaskWithTags? = null,
    val editingRoutineItem: RoutineItem? = null,
    val editingRoutine: com.tasktracker.data.database.entities.Routine? = null,
    val sessionState: SessionState? = null,
    val taskPlayState: TaskPlayState? = null
)

private data class TasksConfig(
    val selectedTagId: Long?,
    val showAddTask: Boolean,
    val showManageTags: Boolean,
    val editingTask: TaskWithTags?,
    val editingRoutineItem: RoutineItem?,
    val editingRoutine: com.tasktracker.data.database.entities.Routine? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val taskRepo: TaskRepository,
    private val routineRepo: RoutineRepository,
    private val sessionLogRepo: SessionLogRepository? = null
) : ViewModel() {

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    private val _showAddTask = MutableStateFlow(false)
    private val _showManageTags = MutableStateFlow(false)
    private val _editingTask = MutableStateFlow<TaskWithTags?>(null)
    private val _editingRoutineItem = MutableStateFlow<RoutineItem?>(null)
    private val _editingRoutine = MutableStateFlow<com.tasktracker.data.database.entities.Routine?>(null)
    private val _sessionState = MutableStateFlow<SessionState?>(null)
    private val _editingRoutineId = MutableStateFlow<Long?>(null)
    val editingRoutineId: StateFlow<Long?> = _editingRoutineId.asStateFlow()

    private val _taskPlayState = MutableStateFlow<TaskPlayState?>(null)
    val taskPlayState: StateFlow<TaskPlayState?> = _taskPlayState.asStateFlow()

    private var timerJob: Job? = null
    private var taskTimerJob: Job? = null

    val uiState: StateFlow<TasksUiState> = combine(
        _selectedTagId,
        _showAddTask,
        _showManageTags,
        combine(_editingTask, _editingRoutineItem, _editingRoutine) { et, eri, er -> Triple(et, eri, er) }
    ) { selectedTag, showAdd, showManage, (editing, editingItem, editingRoutine) ->
        TasksConfig(selectedTag, showAdd, showManage, editing, editingItem, editingRoutine)
    }.flatMapLatest { config ->
        val today = LocalDate.now().toEpochDay()
        combine(
            if (config.selectedTagId == null) taskRepo.getAllTasksWithTags()
            else taskRepo.getTasksWithTagsByTagId(config.selectedTagId),
            taskRepo.getAllTags(),
            routineRepo.getAllRoutinesWithItems(),
            routineRepo.getCompletionsForDate(today)
        ) { tasks, tags, routineList, completions ->
            val routinesWithProgress = routineList.map { rwi ->
                RoutineWithProgress(
                    routine = rwi.routine,
                    items = rwi.items.sortedBy { it.orderIndex }.map { item ->
                        RoutineItemWithCompletion(
                            item = item,
                            isCompleted = completions.any {
                                it.routineId == rwi.routine.id && it.routineItemId == item.id && it.isCompleted
                            }
                        )
                    }
                )
            }
            TasksUiState(
                tasks = tasks,
                tags = tags,
                routines = routinesWithProgress,
                selectedTagId = config.selectedTagId,
                showAddTaskDialog = config.showAddTask,
                showManageTagsDialog = config.showManageTags,
                editingTask = config.editingTask,
                editingRoutineItem = config.editingRoutineItem,
                editingRoutine = config.editingRoutine,
                sessionState = _sessionState.value
            )
        }
    }.combine(_sessionState) { uiState, session ->
        uiState.copy(sessionState = session)
    }.combine(_editingRoutine) { uiState, editingRoutine ->
        uiState.copy(editingRoutine = editingRoutine)
    }.combine(_taskPlayState) { uiState, taskPlay ->
        uiState.copy(taskPlayState = taskPlay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    // Task management
    fun selectTag(tagId: Long?) { _selectedTagId.value = tagId }
    fun showAddTaskDialog() { _showAddTask.value = true }
    fun showManageTagsDialog() { _showManageTags.value = true }
    fun showEditDialog(task: TaskWithTags) { _editingTask.value = task }

    fun dismissDialogs() {
        _showAddTask.value = false
        _showManageTags.value = false
        _editingTask.value = null
        _editingRoutineItem.value = null
        _editingRoutine.value = null
    }

    fun saveTask(task: Task, tagIds: List<Long>, recurrence: com.tasktracker.ui.components.RecurrenceDraft) {
        viewModelScope.launch {
            taskRepo.saveTask(task, tagIds)
            dismissDialogs()
        }
    }

    fun toggleTaskComplete(taskWithTags: TaskWithTags) {
        viewModelScope.launch { taskRepo.toggleTaskComplete(taskWithTags.task) }
    }

    fun deleteTask(taskWithTags: TaskWithTags) {
        viewModelScope.launch { taskRepo.deleteTask(taskWithTags.task) }
    }

    fun saveTag(tag: Tag) { viewModelScope.launch { taskRepo.saveTag(tag) } }
    fun updateTag(tag: Tag) { viewModelScope.launch { taskRepo.updateTag(tag) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch { taskRepo.deleteTag(tag) } }

    // Routine management
    fun showEditRoutineDialog(routine: com.tasktracker.data.database.entities.Routine) {
        _editingRoutine.value = routine
    }

    fun saveEditedRoutine(routine: com.tasktracker.data.database.entities.Routine) {
        viewModelScope.launch {
            routineRepo.updateRoutine(routine)
            _editingRoutine.value = null
        }
    }

    fun addRoutine(
        routine: com.tasktracker.data.database.entities.Routine,
        items: List<com.tasktracker.data.database.entities.RoutineItem>,
        recurrence: com.tasktracker.ui.components.RecurrenceDraft
    ) {
        viewModelScope.launch {
            val routineId = routineRepo.saveRoutine(routine)
            items.forEachIndexed { index, item ->
                routineRepo.saveRoutineItem(item.copy(routineId = routineId, orderIndex = index))
            }
        }
    }

    fun showEditRoutineItemDialog(item: RoutineItem) { _editingRoutineItem.value = item }

    fun saveRoutineItem(item: RoutineItem) {
        viewModelScope.launch {
            routineRepo.updateRoutineItem(item)
            _editingRoutineItem.value = null
        }
    }

    fun deleteRoutineItem(item: RoutineItem) {
        viewModelScope.launch { routineRepo.deleteRoutineItem(item) }
    }

    fun selectRoutineToEdit(routineId: Long) {
        _editingRoutineId.value = routineId
    }

    fun clearRoutineToEdit() { _editingRoutineId.value = null }

    fun deleteRoutine(routine: com.tasktracker.data.database.entities.Routine) {
        viewModelScope.launch { routineRepo.softDeleteRoutine(routine.id) }
    }

    fun reorderItem(routineId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { routineRepo.reorderItems(routineId, fromIndex, toIndex) }
    }

    fun toggleRoutineItemComplete(routineId: Long, itemId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            routineRepo.setItemCompletion(routineId, itemId, LocalDate.now().toEpochDay(), !isCompleted)
        }
    }

    // Session management
    fun startSession(routine: RoutineWithProgress) {
        if (routine.items.isEmpty()) return
        _sessionState.value = SessionState(routine = routine)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _sessionState.value ?: break
                if (current.isFinished) break
                _sessionState.value = current.copy(timerSeconds = current.timerSeconds + 1)
            }
        }
    }

    fun sessionNext() {
        val current = _sessionState.value ?: return
        val completedItem = SessionItemResult(
            item = current.routine.items[current.currentItemIndex].item,
            elapsedSeconds = current.timerSeconds
        )
        val newCompleted = current.completedItems + completedItem
        val nextIndex = current.currentItemIndex + 1

        if (nextIndex >= current.routine.items.size) {
            sessionFinish(current, newCompleted)
        } else {
            _sessionState.value = current.copy(
                currentItemIndex = nextIndex,
                timerSeconds = 0,
                completedItems = newCompleted
            )
        }
    }

    fun sessionFinish() {
        val current = _sessionState.value ?: return
        val completedItem = SessionItemResult(
            item = current.routine.items[current.currentItemIndex].item,
            elapsedSeconds = current.timerSeconds
        )
        sessionFinish(current, current.completedItems + completedItem)
    }

    private fun sessionFinish(current: SessionState, allCompleted: List<SessionItemResult>) {
        timerJob?.cancel()
        val sessionId = UUID.randomUUID().toString()
        val dateEpochDay = LocalDate.now().toEpochDay()

        viewModelScope.launch {
            val logs = allCompleted.map { result ->
                RoutineSessionLog(
                    routineId = current.routine.routine.id,
                    routineItemId = result.item.id,
                    sessionId = sessionId,
                    elapsedSeconds = result.elapsedSeconds,
                    dateEpochDay = dateEpochDay
                )
            }
            sessionLogRepo?.saveLogs(logs)

            // Auto-mark all completed items as done for today
            val today = LocalDate.now().toEpochDay()
            allCompleted.forEach { result ->
                routineRepo.setItemCompletion(current.routine.routine.id, result.item.id, today, true)
            }

            val averages = sessionLogRepo?.getAverageTimePerItem(current.routine.routine.id)
                ?.associate { it.routineItemId to it.avgSeconds }
                ?: emptyMap()

            _sessionState.value = current.copy(
                completedItems = allCompleted,
                isFinished = true,
                averages = averages
            )
        }
    }

    fun dismissSession() {
        timerJob?.cancel()
        _sessionState.value = null
    }

    // Task play (single-task timer)
    fun startTaskPlay(task: Task) {
        _taskPlayState.value = TaskPlayState(task = task)
        taskTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _taskPlayState.value ?: break
                if (current.isFinished) break
                _taskPlayState.value = current.copy(timerSeconds = current.timerSeconds + 1)
            }
        }
    }

    fun finishTaskPlay() {
        taskTimerJob?.cancel()
        val current = _taskPlayState.value ?: return
        viewModelScope.launch {
            taskRepo.toggleTaskComplete(current.task.copy(isCompleted = false))
            _taskPlayState.value = current.copy(isFinished = true)
        }
    }

    fun dismissTaskPlay() {
        taskTimerJob?.cancel()
        _taskPlayState.value = null
    }

    class Factory(
        private val taskRepo: TaskRepository,
        private val routineRepo: RoutineRepository,
        private val sessionLogRepo: SessionLogRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TasksViewModel(taskRepo, routineRepo, sessionLogRepo) as T
    }
}
