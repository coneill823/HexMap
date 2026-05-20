package com.tasktracker.ui.screens.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.dao.ItemSessionPoint
import com.tasktracker.data.database.entities.Routine
import com.tasktracker.data.database.entities.RoutineItem
import com.tasktracker.data.repository.RoutineRepository
import com.tasktracker.data.repository.SessionLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class GraphsUiState(
    val allRoutines: List<Routine> = emptyList(),
    val selectedRoutineId: Long? = null,
    val routineItems: List<RoutineItem> = emptyList(),
    val sessionPoints: List<ItemSessionPoint> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class GraphsViewModel(
    private val routineRepo: RoutineRepository,
    private val sessionLogRepo: SessionLogRepository
) : ViewModel() {

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<GraphsUiState> = combine(
        routineRepo.getAllRoutinesWithItems().map { list -> list.map { it.routine } },
        _selectedRoutineId
    ) { routines, selectedId -> routines to selectedId }
        .flatMapLatest { (routines, selectedId) ->
            if (selectedId == null) {
                flowOf(GraphsUiState(allRoutines = routines))
            } else {
                sessionLogRepo.getSessionPointsForRoutine(selectedId)
                    .map { points ->
                        val items = routineRepo.getItemsForRoutine(selectedId)
                        GraphsUiState(
                            allRoutines = routines,
                            selectedRoutineId = selectedId,
                            routineItems = items,
                            sessionPoints = points
                        )
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphsUiState())

    fun selectRoutine(id: Long?) { _selectedRoutineId.value = id }

    class Factory(
        private val routineRepo: RoutineRepository,
        private val sessionLogRepo: SessionLogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GraphsViewModel(routineRepo, sessionLogRepo) as T
    }
}
