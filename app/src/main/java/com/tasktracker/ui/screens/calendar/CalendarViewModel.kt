package com.tasktracker.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.models.RoutineItemWithCompletion
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.data.repository.RecurrenceRepository
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.TaskRepository
import com.tasktracker.ui.components.RecurrenceDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val tasks: List<TaskWithTags> = emptyList(),
    val routines: List<RoutineWithProgress> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val showAddTaskDialog: Boolean = false,
    val showAddRoutineDialog: Boolean = false,
    val editingRoutine: RoutineWithProgress? = null,
    val editingRoutineItem: RoutineItem? = null
)

private data class CalendarConfig(
    val selectedDate: LocalDate,
    val currentMonth: YearMonth,
    val showAddTask: Boolean,
    val showAddRoutine: Boolean,
    val editingRoutine: RoutineWithProgress?,
    val editingRoutineItem: RoutineItem? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val taskRepo: TaskRepository,
    private val routineRepo: RoutineRepository,
    private val recurrenceRepo: RecurrenceRepository? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _showAddTask = MutableStateFlow(false)
    private val _showAddRoutine = MutableStateFlow(false)
    private val _editingRoutine = MutableStateFlow<RoutineWithProgress?>(null)
    private val _editingRoutineItem = MutableStateFlow<RoutineItem?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_selectedDate, _currentMonth, _showAddTask) { d, m, s -> Triple(d, m, s) },
        combine(_showAddRoutine, _editingRoutine, _editingRoutineItem) { sr, er, eri -> Triple(sr, er, eri) }
    ) { (date, month, showAdd), (showRoutine, editing, editingItem) ->
        CalendarConfig(date, month, showAdd, showRoutine, editing, editingItem)
    }.flatMapLatest { config ->
        val epochDay = config.selectedDate.toEpochDay()
        combine(
            taskRepo.getTasksWithTagsByDate(epochDay),
            routineRepo.getAllRoutinesWithItems(),
            routineRepo.getCompletionsForDate(epochDay),
            taskRepo.getAllTags()
        ) { tasks, routines, completions, tags ->
            val routinesWithProgress = routines.map { rwi ->
                RoutineWithProgress(
                    routine = rwi.routine,
                    items = rwi.items.map { item ->
                        RoutineItemWithCompletion(
                            item = item,
                            isCompleted = completions.any {
                                it.routineId == rwi.routine.id &&
                                    it.routineItemId == item.id &&
                                    it.isCompleted
                            }
                        )
                    }
                )
            }
            CalendarUiState(
                selectedDate = config.selectedDate,
                currentMonth = config.currentMonth,
                tasks = tasks,
                routines = routinesWithProgress,
                availableTags = tags,
                showAddTaskDialog = config.showAddTask,
                showAddRoutineDialog = config.showAddRoutine,
                editingRoutine = config.editingRoutine,
                editingRoutineItem = config.editingRoutineItem
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun navigateMonth(delta: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(delta.toLong())
    }

    fun showAddTaskDialog() { _showAddTask.value = true }
    fun showAddRoutineDialog() { _showAddRoutine.value = true }
    fun dismissDialogs() {
        _showAddTask.value = false
        _showAddRoutine.value = false
        _editingRoutine.value = null
        _editingRoutineItem.value = null
    }

    fun showEditRoutineItemDialog(item: RoutineItem) { _editingRoutineItem.value = item }

    fun saveEditedRoutineItem(item: RoutineItem) {
        viewModelScope.launch {
            routineRepo.updateRoutineItem(item)
            _editingRoutineItem.value = null
        }
    }

    fun addTask(task: Task, tagIds: List<Long>, recurrence: com.tasktracker.ui.components.RecurrenceDraft) {
        viewModelScope.launch {
            taskRepo.saveTask(task, tagIds)
            _showAddTask.value = false
        }
    }

    fun toggleTaskComplete(taskWithTags: TaskWithTags) {
        viewModelScope.launch { taskRepo.toggleTaskComplete(taskWithTags.task) }
    }

    fun deleteTask(taskWithTags: TaskWithTags) {
        viewModelScope.launch { taskRepo.deleteTask(taskWithTags.task) }
    }

    fun addRoutine(routine: Routine, items: List<RoutineItem>, recurrence: com.tasktracker.ui.components.RecurrenceDraft) {
        viewModelScope.launch {
            val routineId = routineRepo.saveRoutine(routine)
            items.forEachIndexed { index, item ->
                routineRepo.saveRoutineItem(item.copy(routineId = routineId, orderIndex = index))
            }
            _showAddRoutine.value = false
        }
    }

    fun addItemToRoutine(routineId: Long, item: RoutineItem) {
        viewModelScope.launch { routineRepo.saveRoutineItem(item.copy(routineId = routineId)) }
    }

    fun deleteRoutineItem(item: RoutineItem) {
        viewModelScope.launch { routineRepo.deleteRoutineItem(item) }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { routineRepo.deleteRoutine(routine) }
    }

    fun toggleRoutineItemComplete(
        routineId: Long,
        itemId: Long,
        date: LocalDate,
        currentlyCompleted: Boolean
    ) {
        viewModelScope.launch {
            routineRepo.setItemCompletion(routineId, itemId, date.toEpochDay(), !currentlyCompleted)
        }
    }

    fun createTag(tag: Tag) {
        viewModelScope.launch { taskRepo.saveTag(tag) }
    }

    class Factory(
        private val taskRepo: TaskRepository,
        private val routineRepo: RoutineRepository,
        private val recurrenceRepo: RecurrenceRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(taskRepo, routineRepo, recurrenceRepo) as T
    }
}
