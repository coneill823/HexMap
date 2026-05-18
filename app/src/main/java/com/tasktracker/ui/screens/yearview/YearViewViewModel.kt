package com.tasktracker.ui.screens.yearview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.models.DayCompletionInfo
import com.tasktracker.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class YearViewUiState(
    val year: Int = LocalDate.now().year,
    val dayCompletions: Map<Long, DayCompletionInfo> = emptyMap(),
    val totalRoutineItems: Int = 0,
    val overallCompletionPercent: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
class YearViewViewModel(private val routineRepo: RoutineRepository) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)

    val uiState: StateFlow<YearViewUiState> = _year.flatMapLatest { year ->
        val startDate = LocalDate.of(year, 1, 1).toEpochDay()
        val endDate = LocalDate.of(year, 12, 31).toEpochDay()

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
            val daysElapsed = if (today.year == year) {
                today.dayOfYear
            } else if (today.year > year) {
                if (LocalDate.of(year, 12, 31).isLeapYear()) 366 else 365
            } else {
                0
            }

            val fullyCompletedDays = if (totalItems > 0) {
                counts.count { it.completed >= totalItems }
            } else 0

            val overallPercent = if (daysElapsed > 0 && totalItems > 0) {
                fullyCompletedDays.toFloat() / daysElapsed * 100f
            } else 0f

            YearViewUiState(
                year = year,
                dayCompletions = completionMap,
                totalRoutineItems = totalItems,
                overallCompletionPercent = overallPercent
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearViewUiState())

    fun navigateYear(delta: Int) { _year.value = _year.value + delta }

    class Factory(private val routineRepo: RoutineRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            YearViewViewModel(routineRepo) as T
    }
}

private fun LocalDate.isLeapYear(): Boolean = java.time.Year.of(year).isLeap
