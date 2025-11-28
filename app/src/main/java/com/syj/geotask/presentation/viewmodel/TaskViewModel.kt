package com.syj.geotask.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.usecase.AddTaskUseCase
import com.syj.geotask.domain.usecase.DeleteTaskUseCase
import com.syj.geotask.domain.usecase.GetTasksUseCase
import com.syj.geotask.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var filterType by mutableStateOf(FilterType.ALL)
        private set

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tasks = combine(
                    when (filterType) {
                        FilterType.ALL -> getTasksUseCase.getAllTasks()
                        FilterType.COMPLETED -> getTasksUseCase.getTasksByCompletionStatus(true)
                        FilterType.INCOMPLETE -> getTasksUseCase.getTasksByCompletionStatus(false)
                    },
                    getTasksUseCase.searchTasks(searchQuery)
                ) { allTasks, searchedTasks ->
                    if (searchQuery.isBlank()) {
                        allTasks
                    } else {
                        searchedTasks
                    }
                }.first()
                _tasks.value = tasks

            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        loadTasks()
    }

    fun onFilterTypeChanged(filterType: FilterType) {
        this.filterType = filterType
        loadTasks()
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                addTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task.copy(isCompleted = !task.isCompleted))
                loadTasks()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleTaskReminder(taskId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val task = getTasksUseCase.getTaskById(taskId)
                task?.let {
                    updateTaskUseCase(it.copy(isReminderEnabled = isEnabled))
                    loadTasks()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    suspend fun getTaskById(id: Long): Task? {
        return try {
            getTasksUseCase.getTaskById(id)
        } catch (e: Exception) {
            null
        }
    }
}

enum class FilterType {
    ALL, COMPLETED, INCOMPLETE
}
