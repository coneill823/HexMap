package com.tasktracker.ui.screens.yearview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.models.DayCompletionInfo
import com.tasktracker.data.models.OverviewGranularity
import com.tasktracker.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val overallCompletionPercent: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
class YearViewViewModel(private val routineRepo: RoutineRepository) : ViewModel() {

    private val _granularity = MutableStateFlow(OverviewGranularity.YEAR)
    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(YearMonth.now())
    private val _weekStart = MutableStateFlow(
        LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    )

    val uiState: StateFlow<YearViewUiState> = combine(
        _granularity,
        _year,
        combine(_month, _weekStart) { m, w -> m to w }
    ) { gran, year, (month, weekStart) ->
        Triple(gran, year, month to weekStart)
    }.flatMapLatest { (gran, year, monthAndWeek) ->
        val (month, weekStart) = monthAndWeek
        val (startDate, endDate) = when (gran) {
            OverviewGranularity.YEAR -> {
                LocalDate.of(year, 1, 1).toEpochDay() to LocalDate.of(year, 12, 31).toEpochDay()
            }
            OverviewGranularity.MONTH -> {
                month.atDay(1).toEpochDay() to month.atEndOfMonth().toEpochDay()
            }
            OverviewGranularity.WEEK -> {
                weekStart.toEpochDay() to weekStart.plusDays(6).toEpochDay()
            }
        }

        routineRepo.getCompletedCountsByDateRange(startDate, endDate).map { counts ->
            val totalItems = routineRepo.getTotalItemCount()
            val completionMap = mutableMapOf<Long, DayCompletionInfo>()
            counts.forEach { dateCount ->
                completionMap[dateCount.dateEpochDay] = DayCompletionInfo(
                    dateEpochDay = dateCount.dateEpochDay,
                    completedCount = dateCount.completed,
                    totalCount = totalItems
                )
            }

            val today = LocalDate.now()
            val daysElapsed = when {
                gran == OverviewGranularity.YEAR -> {
                    if (today.year == year) today.dayOfYear
                    else if (today.year > year) if (java.time.Year.of(year).isLeap) 366 else 365
                    else 0
                }
                gran == OverviewGranularity.MONTH -> {
                    val endOfMonth = month.atEndOfMonth()
                    if (today.isBefore(month.atDay(1))) 0
                    else if (today.isAfter(endOfMonth)) endOfMonth.dayOfMonth
                    else today.dayOfMonth
                }
                else -> {
                    val weekEnd = weekStart.plusDays(6)
                    when {
                        today.isBefore(weekStart) -> 0
                        today.isAfter(weekEnd) -> 7
                        else -> java.time.temporal.ChronoUnit.DAYS.between(weekStart, today).toInt() + 1
                    }
                }
            }

            val fullyCompletedDays = if (totalItems > 0) counts.count { it.completed >= totalItems } else 0
            val overallPercent = if (daysElapsed > 0 && totalItems > 0) {
                fullyCompletedDays.toFloat() / daysElapsed * 100f
            } else 0f

            YearViewUiState(
                granularity = gran,
                year = year,
                month = month,
                weekStart = weekStart,
                dayCompletions = completionMap,
                totalRoutineItems = totalItems,
                overallCompletionPercent = overallPercent
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearViewUiState())

    fun setGranularity(g: OverviewGranularity) { _granularity.value = g }
    fun navigateYear(delta: Int) { _year.value = _year.value + delta }
    fun navigateMonth(delta: Int) { _month.value = _month.value.plusMonths(delta.toLong()) }
    fun navigateWeek(delta: Int) { _weekStart.value = _weekStart.value.plusWeeks(delta.toLong()) }

    class Factory(private val routineRepo: RoutineRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            YearViewViewModel(routineRepo) as T
    }
}
