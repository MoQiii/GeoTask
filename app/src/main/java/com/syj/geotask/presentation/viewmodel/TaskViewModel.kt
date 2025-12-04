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
import timber.log.Timber
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
    private val deleteTaskWithGeofenceUseCase: DeleteTaskWithGeofenceUseCase,
    private val taskReminderManager: com.syj.geotask.data.service.TaskReminderManager
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
        Timber.d("📍 更新选中位置:")
        Timber.d("  地址: $location")
        Timber.d("  纬度: $latitude")
        Timber.d("  经度: $longitude")
        
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
            Timber.d("💾 开始保存任务:")
            Timber.d("  标题: $taskTitle")
            Timber.d("  描述: $taskDescription")
            Timber.d("  日期: ${selectedDate}")
            Timber.d("  时间: ${selectedTime}")
            Timber.d("  启用提醒: $isReminderEnabled")
            Timber.d("  位置地址: $selectedLocation")
            Timber.d("  纬度: $selectedLatitude")
            Timber.d("  经度: $selectedLongitude")
            
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
            
            Timber.d("📋 创建的任务对象:")
            Timber.d("  title: ${task.title}")
            Timber.d("  description: ${task.description}")
            Timber.d("  dueDate: ${task.dueDate}")
            Timber.d("  dueTime: ${task.dueTime}")
            Timber.d("  isReminderEnabled: ${task.isReminderEnabled}")
            Timber.d("  location: ${task.location}")
            Timber.d("  latitude: ${task.latitude}")
            Timber.d("  longitude: ${task.longitude}")
            Timber.d("  geofenceRadius: ${task.geofenceRadius}")
            
            viewModelScope.launch {
                try {
                    // 保存任务并获取生成的ID
                    val taskId: Long = if (selectedLocation != null && selectedLatitude != null && selectedLongitude != null) {
                        val id = addTaskWithGeofenceUseCase(task)
                        Timber.d("✅ 任务已保存（带地理围栏）: ${task.title}")
                        id
                    } else {
                        val id = addTaskUseCase(task)
                        Timber.d("✅ 任务已保存: ${task.title}")
                        id
                    }
                    
                    // 如果启用了提醒，调度精确提醒
                    if (task.isReminderEnabled) {
                        Timber.d("🔔 开始调度任务提醒: taskId=$taskId, title=${task.title}")
                        taskReminderManager.scheduleTaskReminderForTime(
                            taskId = taskId,
                            dueDate = task.dueDate,
                            dueTime = task.dueTime
                        )
                        Timber.d("✅ 任务提醒调度完成: ${task.title}")
                    } else {
                        Timber.d("⏸️ 任务未启用提醒: ${task.title}")
                    }
                    
                    // 重新加载任务列表
                    loadTasks()
                } catch (e: Exception) {
                    Timber.e(e, "❌ 保存任务失败: ${task.title}")
                }
            }
            
            // 保存后清空表单
            clearTaskForm()
        }
    }
}

enum class FilterType {
    ALL, COMPLETED, INCOMPLETE
}
