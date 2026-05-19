package com.tasktracker.ui.screens.yearview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.models.DayCompletionInfo
import com.tasktracker.data.models.OverviewGranularity
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

data class YearViewUiState(
    val granularity: OverviewGranularity = OverviewGranularity.YEAR,
    val year: Int = LocalDate.now().year,
    val month: YearMonth = YearMonth.now(),
    val weekStart: LocalDate = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1),
    val dayCompletions: Map<Long, DayCompletionInfo> = emptyMap(),
    val totalRoutineItems: Int = 0,
    val overallCompletionPercent: Float = 0f,
    // Filter
    val allRoutines: List<Routine> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val selectedRoutineId: Long? = null,
    val selectedTagId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class YearViewViewModel(
    private val routineRepo: RoutineRepository,
    private val taskRepo: TaskRepository? = null
) : ViewModel() {

    private val _granularity = MutableStateFlow(OverviewGranularity.YEAR)
    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(YearMonth.now())
    private val _weekStart = MutableStateFlow(
        LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    )
    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    private val _selectedTagId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<YearViewUiState> = combine(
        _granularity,
        _year,
        combine(_month, _weekStart) { m, w -> m to w },
        combine(_selectedRoutineId, _selectedTagId) { r, t -> r to t }
    ) { gran, year, (month, weekStart), (routineId, tagId) ->
        object {
            val g = gran; val y = year; val m = month; val ws = weekStart
            val rid = routineId; val tid = tagId
        }
    }.flatMapLatest { c ->
        val (startDate, endDate) = when (c.g) {
            OverviewGranularity.YEAR ->
                LocalDate.of(c.y, 1, 1).toEpochDay() to LocalDate.of(c.y, 12, 31).toEpochDay()
            OverviewGranularity.MONTH ->
                c.m.atDay(1).toEpochDay() to c.m.atEndOfMonth().toEpochDay()
            OverviewGranularity.WEEK ->
                c.ws.toEpochDay() to c.ws.plusDays(6).toEpochDay()
        }

        val countsFlow: Flow<List<com.tasktracker.data.database.dao.DateCompletedCount>> = when {
            c.rid != null -> routineRepo.getCompletedCountsByDateRangeAndRoutine(c.rid, startDate, endDate)
            c.tid != null -> taskRepo?.run {
                getCompletedTaskCountsByTagAndDateRange(c.tid, startDate, endDate)
            } ?: flowOf(emptyList())
            else -> routineRepo.getCompletedCountsByDateRange(startDate, endDate)
        }

        val routinesFlow = routineRepo.getAllRoutinesWithItems().map { list -> list.map { it.routine } }
        val tagsFlow = taskRepo?.getAllTags() ?: flowOf(emptyList())

        combine(countsFlow, routinesFlow, tagsFlow) { counts, routines, tags ->
            val totalItems = when {
                c.rid != null -> routineRepo.getItemCountForRoutine(c.rid)
                c.tid != null -> taskRepo?.getTaskCountByTagAndDateRange(c.tid, startDate, endDate) ?: 0
                else -> routineRepo.getTotalItemCount()
            }

            val completionMap = mutableMapOf<Long, DayCompletionInfo>()
            counts.forEach { dc ->
                completionMap[dc.dateEpochDay] = DayCompletionInfo(
                    dateEpochDay = dc.dateEpochDay,
                    completedCount = dc.completed,
                    totalCount = totalItems
                )
            }

            val today = LocalDate.now()
            val daysElapsed = when (c.g) {
                OverviewGranularity.YEAR -> {
                    if (today.year == c.y) today.dayOfYear
                    else if (today.year > c.y) if (java.time.Year.of(c.y).isLeap) 366 else 365
                    else 0
                }
                OverviewGranularity.MONTH -> {
                    val end = c.m.atEndOfMonth()
                    if (today.isBefore(c.m.atDay(1))) 0
                    else if (today.isAfter(end)) end.dayOfMonth
                    else today.dayOfMonth
                }
                else -> {
                    val weekEnd = c.ws.plusDays(6)
                    when {
                        today.isBefore(c.ws) -> 0
                        today.isAfter(weekEnd) -> 7
                        else -> java.time.temporal.ChronoUnit.DAYS.between(c.ws, today).toInt() + 1
                    }
                }
            }

            val fullyCompletedDays = if (totalItems > 0) counts.count { it.completed >= totalItems } else 0
            val overallPercent = if (daysElapsed > 0 && totalItems > 0) {
                fullyCompletedDays.toFloat() / daysElapsed * 100f
            } else 0f

            YearViewUiState(
                granularity = c.g,
                year = c.y,
                month = c.m,
                weekStart = c.ws,
                dayCompletions = completionMap,
                totalRoutineItems = totalItems,
                overallCompletionPercent = overallPercent,
                allRoutines = routines,
                allTags = tags,
                selectedRoutineId = c.rid,
                selectedTagId = c.tid
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearViewUiState())

    fun setGranularity(g: OverviewGranularity) { _granularity.value = g }
    fun navigateYear(delta: Int) { _year.value = _year.value + delta }
    fun navigateMonth(delta: Int) { _month.value = _month.value.plusMonths(delta.toLong()) }
    fun navigateWeek(delta: Int) { _weekStart.value = _weekStart.value.plusWeeks(delta.toLong()) }
    fun selectRoutine(id: Long?) { _selectedRoutineId.value = id; if (id != null) _selectedTagId.value = null }
    fun selectTag(id: Long?) { _selectedTagId.value = id; if (id != null) _selectedRoutineId.value = null }

    class Factory(
        private val routineRepo: RoutineRepository,
        private val taskRepo: TaskRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            YearViewViewModel(routineRepo, taskRepo) as T
    }
}
