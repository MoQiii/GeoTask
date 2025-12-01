package com.syj.geotask.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.usecase.AddTaskUseCase
import com.syj.geotask.domain.usecase.AddTaskWithGeofenceUseCase
import com.syj.geotask.domain.usecase.DeleteTaskUseCase
import com.syj.geotask.domain.usecase.DeleteTaskWithGeofenceUseCase
import com.syj.geotask.domain.usecase.GetTasksUseCase
import com.syj.geotask.domain.usecase.UpdateTaskUseCase
import com.syj.geotask.domain.usecase.UpdateTaskWithGeofenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val addTaskWithGeofenceUseCase: AddTaskWithGeofenceUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val updateTaskWithGeofenceUseCase: UpdateTaskWithGeofenceUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val deleteTaskWithGeofenceUseCase: DeleteTaskWithGeofenceUseCase
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var filterType by mutableStateOf(FilterType.ALL)
        private set

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 添加任务表单状态
    var taskTitle by mutableStateOf("")
        private set
    
    var taskDescription by mutableStateOf("")
        private set
    
    var selectedDate by mutableStateOf(Date())
        private set
    
    var selectedTime by mutableStateOf(Date())
        private set
    
    var isReminderEnabled by mutableStateOf(false)
        private set
    
    var selectedLocation by mutableStateOf<String?>(null)
        private set
    
    var selectedLatitude by mutableStateOf<Double?>(null)
        private set
    
    var selectedLongitude by mutableStateOf<Double?>(null)
        private set

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

    fun addTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                addTaskWithGeofenceUseCase(task)
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

    fun updateTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskWithGeofenceUseCase(task)
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

    fun deleteTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                deleteTaskWithGeofenceUseCase(task)
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

    // 表单状态更新方法
    fun updateTaskTitle(title: String) {
        taskTitle = title
    }

    fun updateTaskDescription(description: String) {
        taskDescription = description
    }

    fun updateSelectedDate(date: Date) {
        selectedDate = date
    }

    fun updateSelectedTime(time: Date) {
        selectedTime = time
    }

    fun updateReminderEnabled(enabled: Boolean) {
        isReminderEnabled = enabled
    }

    fun updateSelectedLocation(location: String?, latitude: Double?, longitude: Double?) {
        selectedLocation = location
        selectedLatitude = latitude
        selectedLongitude = longitude
    }

    // 清空表单状态
    fun clearTaskForm() {
        taskTitle = ""
        taskDescription = ""
        selectedDate = Date()
        selectedTime = Date()
        isReminderEnabled = false
        selectedLocation = null
        selectedLatitude = null
        selectedLongitude = null
    }

    // 创建并保存任务
    fun saveTask() {
        if (taskTitle.isNotBlank()) {
            val task = Task(
                title = taskTitle,
                description = taskDescription,
                dueDate = selectedDate.time,
                dueTime = selectedTime.time,
                isReminderEnabled = isReminderEnabled,
                location = selectedLocation,
                latitude = selectedLatitude,
                longitude = selectedLongitude
            )
            
            // 如果有位置信息，使用带地理围栏的方法
            if (selectedLocation != null && selectedLatitude != null && selectedLongitude != null) {
                addTaskWithGeofence(task)
            } else {
                addTask(task)
            }
            
            // 保存后清空表单
            clearTaskForm()
        }
    }
}

enum class FilterType {
    ALL, COMPLETED, INCOMPLETE
}
