package com.tasktracker.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasktracker.data.database.entities.Tag
import com.tasktracker.data.database.entities.Task
import com.tasktracker.data.models.TaskWithTags
import com.tasktracker.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<TaskWithTags> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val showAddTaskDialog: Boolean = false,
    val showManageTagsDialog: Boolean = false,
    val editingTask: TaskWithTags? = null
)

private data class TasksConfig(
    val selectedTagId: Long?,
    val showAddTask: Boolean,
    val showManageTags: Boolean,
    val editingTask: TaskWithTags?
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(private val taskRepo: TaskRepository) : ViewModel() {

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    private val _showAddTask = MutableStateFlow(false)
    private val _showManageTags = MutableStateFlow(false)
    private val _editingTask = MutableStateFlow<TaskWithTags?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        _selectedTagId,
        _showAddTask,
        _showManageTags,
        _editingTask
    ) { selectedTag, showAdd, showManage, editing ->
        TasksConfig(selectedTag, showAdd, showManage, editing)
    }.flatMapLatest { config ->
        combine(
            if (config.selectedTagId == null) taskRepo.getAllTasksWithTags()
            else taskRepo.getTasksWithTagsByTagId(config.selectedTagId),
            taskRepo.getAllTags()
        ) { tasks, tags ->
            TasksUiState(
                tasks = tasks,
                tags = tags,
                selectedTagId = config.selectedTagId,
                showAddTaskDialog = config.showAddTask,
                showManageTagsDialog = config.showManageTags,
                editingTask = config.editingTask
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun selectTag(tagId: Long?) { _selectedTagId.value = tagId }

    fun showAddTaskDialog() { _showAddTask.value = true }
    fun showManageTagsDialog() { _showManageTags.value = true }
    fun showEditDialog(task: TaskWithTags) { _editingTask.value = task }

    fun dismissDialogs() {
        _showAddTask.value = false
        _showManageTags.value = false
        _editingTask.value = null
    }

    fun saveTask(task: Task, tagIds: List<Long>) {
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

    class Factory(private val taskRepo: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TasksViewModel(taskRepo) as T
    }
}
