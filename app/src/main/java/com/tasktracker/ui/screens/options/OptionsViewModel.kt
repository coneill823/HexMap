package com.tasktracker.ui.screens.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.models.AppTheme
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.ThemeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OptionsUiState(
    val currentTheme: AppTheme = AppTheme.PURPLE,
    val deletedRoutines: List<Routine> = emptyList(),
    val selectedTab: Int = 0
)

class OptionsViewModel(
    private val themeRepo: ThemeRepository,
    private val routineRepo: RoutineRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<OptionsUiState> = combine(
        themeRepo.currentTheme,
        routineRepo.getDeletedRoutines(),
        _selectedTab
    ) { theme, deleted, tab ->
        OptionsUiState(currentTheme = theme, deletedRoutines = deleted, selectedTab = tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OptionsUiState())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { themeRepo.setTheme(theme) }
    }

    fun restoreRoutine(routineId: Long) {
        viewModelScope.launch { routineRepo.restoreRoutine(routineId) }
    }

    fun selectTab(tab: Int) { _selectedTab.value = tab }

    class Factory(
        private val themeRepo: ThemeRepository,
        private val routineRepo: RoutineRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OptionsViewModel(themeRepo, routineRepo) as T
    }
}
