package com.tasktracker.ui.screens.calendar

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.RecurrenceRule
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.database.entities.RoutineSessionLog
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.models.RoutineItemWithCompletion
import com.tasktracker.data.models.RoutineWithProgress
import com.tasktracker.data.models.SessionItemResult
import com.tasktracker.data.models.SessionState
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.data.repository.RecurrenceRepository
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.SessionLogRepository
import com.tasktracker.data.repository.TaskRepository
import com.tasktracker.ui.components.RecurrenceDraft
import com.tasktracker.widget.RoutineStartWidgetReceiver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val tasks: List<TaskWithTags> = emptyList(),
    val routines: List<RoutineWithProgress> = emptyList(),
    val allRoutines: List<RoutineWithProgress> = emptyList(),
    val routineRules: Map<Long, RecurrenceRule> = emptyMap(),
    val availableTags: List<Tag> = emptyList(),
    val showAddTaskDialog: Boolean = false,
    val showAddRoutineDialog: Boolean = false,
    val editingRoutine: RoutineWithProgress? = null,
    val editingRoutineItem: RoutineItem? = null,
    val sessionState: SessionState? = null
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
    application: Application,
    private val taskRepo: TaskRepository,
    private val routineRepo: RoutineRepository,
    private val recurrenceRepo: RecurrenceRepository? = null,
    private val sessionLogRepo: SessionLogRepository? = null
) : AndroidViewModel(application) {

    private fun updateRoutineWidget() {
        try {
            val ctx = getApplication<Application>()
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, RoutineStartWidgetReceiver::class.java))
            if (ids.isNotEmpty()) {
                ctx.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = ComponentName(ctx, RoutineStartWidgetReceiver::class.java)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        } catch (_: Exception) {}
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _showAddTask = MutableStateFlow(false)
    private val _showAddRoutine = MutableStateFlow(false)
    private val _editingRoutine = MutableStateFlow<RoutineWithProgress?>(null)
    private val _editingRoutineItem = MutableStateFlow<RoutineItem?>(null)
    private val _sessionState = MutableStateFlow<SessionState?>(null)
    private var timerJob: Job? = null

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_selectedDate, _currentMonth, _showAddTask) { d, m, s -> Triple(d, m, s) },
        combine(_showAddRoutine, _editingRoutine, _editingRoutineItem) { sr, er, eri -> Triple(sr, er, eri) }
    ) { (date, month, showAdd), (showRoutine, editing, editingItem) ->
        CalendarConfig(date, month, showAdd, showRoutine, editing, editingItem)
    }.flatMapLatest { config ->
        val epochDay = config.selectedDate.toEpochDay()
        val allRulesFlow: Flow<List<RecurrenceRule>> =
            recurrenceRepo?.getAllRules() ?: flowOf(emptyList())

        combine(
            combine(
                taskRepo.getTasksWithTagsByDate(epochDay),
                routineRepo.getAllRoutinesWithItems(),
                routineRepo.getCompletionsForDate(epochDay)
            ) { tasks, routines, completions -> Triple(tasks, routines, completions) },
            combine(
                taskRepo.getAllTags(),
                allRulesFlow,
                taskRepo.getAllTasksWithTags()
            ) { tags, rules, allTasks -> Triple(tags, rules, allTasks) }
        ) { (dateTasksRaw, routinesRaw, completions), (tags, rules, allTasksRaw) ->

            // Build rule lookup maps
            val routineRulesMap = rules
                .filter { it.ownerType == "routine" }
                .associateBy { it.ownerId }
            val taskRulesMap = rules
                .filter { it.ownerType == "task" }
                .associateBy { it.ownerId }

            // Build all routines with progress (unfiltered)
            val allRoutinesWithProgress = routinesRaw.map { rwi ->
                RoutineWithProgress(
                    routine = rwi.routine,
                    items = rwi.items.sortedBy { it.orderIndex }.map { item ->
                        RoutineItemWithCompletion(
                            item = item,
                            isCompleted = completions.any {
                                it.routineId == rwi.routine.id &&
                                    it.routineItemId == item.id &&
                                    it.isCompleted
                            }
                        )
                    },
                    tags = rwi.tags
                )
            }

            // Filter routines for selected date using recurrence rules
            val filteredRoutines = allRoutinesWithProgress.filter { rwp ->
                val rule = routineRulesMap[rwp.routine.id]
                rule?.occursOn(config.selectedDate) ?: true
            }

            // Tasks from date-specific query (scheduledDate == epochDay)
            val dateTaskIds = dateTasksRaw.map { it.task.id }.toSet()

            // Also include recurring tasks that occur on selectedDate and have no scheduledDate
            val recurringTasksForDate = allTasksRaw.filter { twt ->
                val task = twt.task
                if (task.id in dateTaskIds) return@filter false
                val taskRule = taskRulesMap[task.id] ?: return@filter false
                val hasNoScheduledDate = task.scheduledDate == null || task.scheduledDate == 0L
                hasNoScheduledDate && taskRule.occursOn(config.selectedDate)
            }

            // One-time tasks with no scheduled date and no recurrence rule — always pending
            val unscheduledOneTimeTasks = allTasksRaw.filter { twt ->
                val task = twt.task
                if (task.id in dateTaskIds) return@filter false
                if (taskRulesMap.containsKey(task.id)) return@filter false
                task.scheduledDate == null || task.scheduledDate == 0L
            }

            // Merge and deduplicate by task ID
            val mergedTasksById = LinkedHashMap<Long, TaskWithTags>()
            dateTasksRaw.forEach { mergedTasksById[it.task.id] = it }
            recurringTasksForDate.forEach { mergedTasksById[it.task.id] = it }
            unscheduledOneTimeTasks.forEach { mergedTasksById[it.task.id] = it }
            val filteredTasks = mergedTasksById.values.toList()

            CalendarUiState(
                selectedDate = config.selectedDate,
                currentMonth = config.currentMonth,
                tasks = filteredTasks,
                routines = filteredRoutines,
                allRoutines = allRoutinesWithProgress,
                routineRules = routineRulesMap,
                availableTags = tags,
                showAddTaskDialog = config.showAddTask,
                showAddRoutineDialog = config.showAddRoutine,
                editingRoutine = config.editingRoutine,
                editingRoutineItem = config.editingRoutineItem
            )
        }
    }.combine(_sessionState) { uiState, session ->
        uiState.copy(sessionState = session)
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
            allCompleted.forEach { result ->
                routineRepo.setItemCompletion(current.routine.routine.id, result.item.id, dateEpochDay, true)
            }
            val averages = sessionLogRepo?.getAverageTimePerItem(current.routine.routine.id)
                ?.associate { it.routineItemId to it.avgSeconds } ?: emptyMap()
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

    fun showEditRoutineItemDialog(item: RoutineItem) { _editingRoutineItem.value = item }

    fun saveEditedRoutineItem(item: RoutineItem) {
        viewModelScope.launch {
            routineRepo.updateRoutineItem(item)
            _editingRoutineItem.value = null
            updateRoutineWidget()
        }
    }

    fun addTask(task: Task, tagIds: List<Long>, recurrence: com.tasktracker.ui.components.RecurrenceDraft) {
        viewModelScope.launch {
            val taskId = taskRepo.saveTask(task, tagIds)
            if (recurrence.enabled) {
                val startDay = task.scheduledDate ?: LocalDate.now().toEpochDay()
                recurrenceRepo?.saveRule(recurrence.toRule(taskId, "task", startDay))
            }
            _showAddTask.value = false
            updateRoutineWidget()
        }
    }

    fun toggleTaskComplete(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            taskRepo.toggleTaskComplete(taskWithTags.task)
            updateRoutineWidget()
        }
    }

    fun deleteTask(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            taskRepo.deleteTask(taskWithTags.task)
            updateRoutineWidget()
        }
    }

    fun addRoutine(routine: Routine, items: List<RoutineItem>, recurrence: com.tasktracker.ui.components.RecurrenceDraft, tagIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val routineId = routineRepo.saveRoutine(routine)
            items.forEachIndexed { index, item ->
                routineRepo.saveRoutineItem(item.copy(routineId = routineId, orderIndex = index))
            }
            if (recurrence.enabled) {
                recurrenceRepo?.saveRule(recurrence.toRule(routineId, "routine", LocalDate.now().toEpochDay()))
            }
            routineRepo.saveRoutineTags(routineId, tagIds)
            _showAddRoutine.value = false
            updateRoutineWidget()
        }
    }

    fun addItemToRoutine(routineId: Long, item: RoutineItem) {
        viewModelScope.launch { routineRepo.saveRoutineItem(item.copy(routineId = routineId)) }
    }

    fun deleteRoutineItem(item: RoutineItem) {
        viewModelScope.launch { routineRepo.deleteRoutineItem(item) }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            routineRepo.deleteRoutine(routine)
            updateRoutineWidget()
        }
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
        private val application: Application,
        private val taskRepo: TaskRepository,
        private val routineRepo: RoutineRepository,
        private val recurrenceRepo: RecurrenceRepository? = null,
        private val sessionLogRepo: SessionLogRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(application, taskRepo, routineRepo, recurrenceRepo, sessionLogRepo) as T
    }
}
