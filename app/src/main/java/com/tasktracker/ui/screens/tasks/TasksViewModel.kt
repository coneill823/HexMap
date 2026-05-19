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

data class TasksUiState(
    val tasks: List<TaskWithTags> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val routines: List<RoutineWithProgress> = emptyList(),
    val selectedTagId: Long? = null,
    val showAddTaskDialog: Boolean = false,
    val showManageTagsDialog: Boolean = false,
    val editingTask: TaskWithTags? = null,
    val editingRoutineItem: RoutineItem? = null,
    val sessionState: SessionState? = null
)

private data class TasksConfig(
    val selectedTagId: Long?,
    val showAddTask: Boolean,
    val showManageTags: Boolean,
    val editingTask: TaskWithTags?,
    val editingRoutineItem: RoutineItem?
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
    private val _sessionState = MutableStateFlow<SessionState?>(null)
    private val _editingRoutineId = MutableStateFlow<Long?>(null)
    val editingRoutineId: StateFlow<Long?> = _editingRoutineId.asStateFlow()

    private var timerJob: Job? = null

    val uiState: StateFlow<TasksUiState> = combine(
        _selectedTagId,
        _showAddTask,
        _showManageTags,
        combine(_editingTask, _editingRoutineItem) { et, eri -> et to eri }
    ) { selectedTag, showAdd, showManage, (editing, editingItem) ->
        TasksConfig(selectedTag, showAdd, showManage, editing, editingItem)
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
                sessionState = _sessionState.value
            )
        }
    }.combine(_sessionState) { uiState, session ->
        uiState.copy(sessionState = session)
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
        viewModelScope.launch { routineRepo.deleteRoutine(routine) }
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
