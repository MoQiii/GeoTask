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
import com.syj.geotask.speech.VoiceTaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val taskReminderManager: com.syj.geotask.data.service.TaskReminderManager,
    private val voiceTaskManager: VoiceTaskManager
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var filterType by mutableStateOf(FilterType.ALL)
        private set

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 语音任务状态 - 直接暴露 VoiceTaskManager 的 StateFlow
    val isVoiceRecording: StateFlow<Boolean> = voiceTaskManager.isRecording
    val isVoiceProcessing: StateFlow<Boolean> = voiceTaskManager.isProcessing
    val voiceErrorMessage: StateFlow<String?> = voiceTaskManager.errorMessage

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
    
    var geofenceRadius by mutableStateOf(200f)
        private set

    init {
        // 初始加载任务
        loadTasks()
        
        // 初始化语音任务管理器
        initializeVoiceTaskManager()
    }

    /**
     * 初始化语音任务管理器
     */
    private fun initializeVoiceTaskManager() {
        viewModelScope.launch {
            try {
                val initialized = voiceTaskManager.initialize()
                if (initialized) {
                    Timber.d("语音任务管理器初始化成功")
                } else {
                    Timber.e("语音任务管理器初始化失败")
                }
            } catch (e: Exception) {
                Timber.e(e, "初始化语音任务管理器时发生错误")
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Timber.d("ViewModel开始加载任务")
                Timber.d("  过滤类型: $filterType")
                Timber.d("  搜索查询: '$searchQuery'")
                
                val tasksFlow = when (filterType) {
                    FilterType.ALL -> getTasksUseCase.getAllTasks()
                    FilterType.COMPLETED -> getTasksUseCase.getTasksByCompletionStatus(true)
                    FilterType.INCOMPLETE -> getTasksUseCase.getTasksByCompletionStatus(false)
                }
                
                // 如果有搜索查询，使用搜索结果，否则使用过滤结果
                val finalFlow = if (searchQuery.isBlank()) {
                    tasksFlow
                } else {
                    getTasksUseCase.searchTasks(searchQuery)
                }
                
                Timber.d("📡 开始从Flow收集数据")
                finalFlow.collect { tasks ->
                    Timber.d("ViewModel收到任务列表: ${tasks.size} 个任务")
                    Timber.d("  任务详情: ${tasks.map { "${it.id}:${it.title}" }}")
                    _tasks.value = tasks
                    _isLoading.value = false
                    Timber.d("ViewModel已更新任务状态")
                    return@collect
                }

            } catch (e: Exception) {
                Timber.e(e, "ViewModel加载任务失败")
                _tasks.value = emptyList()
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

    /**
     * 开始语音录音
     */
    suspend fun startVoiceRecording(): Boolean {
        Timber.d("TaskViewModel.startVoiceRecording() 被调用")
        return try {
            voiceTaskManager.clearError()
            Timber.d("清除错误状态完成，开始调用 voiceTaskManager.startRecording()")
            val success = voiceTaskManager.startRecording()
            if (success) {
                Timber.d("开始语音录音成功")
            } else {
                Timber.e("开始语音录音失败")
            }
            success
        } catch (e: Exception) {
            Timber.e(e, "开始语音录音时发生错误")
            false
        }
    }

    /**
     * 停止语音录音并处理
     */
    suspend fun stopVoiceRecordingAndProcess(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            voiceTaskManager.stopRecordingAndProcess(
                onSuccess = { recognizedText ->
                    Timber.d("语音任务创建成功: $recognizedText")
                    // 重新加载任务列表以显示新创建的任务
                    loadTasks()
                    onSuccess(recognizedText)
                },
                onError = { errorMsg ->
                    Timber.e("语音任务创建失败: $errorMsg")
                    onError(errorMsg)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "停止语音录音时发生错误")
            onError("处理语音录音失败: ${e.message}")
        }
    }

    /**
     * 取消语音录音
     */
    suspend fun cancelVoiceRecording() {
        try {
            voiceTaskManager.cancelRecording()
            Timber.d("已取消语音录音")
        } catch (e: Exception) {
            Timber.e(e, "取消语音录音时发生错误")
        }
    }

    /**
     * 清除语音错误消息
     */
    fun clearVoiceError() {
        voiceTaskManager.clearError()
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                val newTaskId = addTaskUseCase(task)
                // 本地更新状态，避免重新加载
                val newTask = task.copy(id = newTaskId)
                val currentTasks = _tasks.value.toMutableList()
                currentTasks.add(newTask)
                _tasks.value = currentTasks
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun addTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                val newTaskId = addTaskWithGeofenceUseCase(task)
                // 本地更新状态，避免重新加载
                val newTask = task.copy(id = newTaskId)
                val currentTasks = _tasks.value.toMutableList()
                currentTasks.add(newTask)
                _tasks.value = currentTasks
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task)
                // 本地更新状态，避免重新加载
                val currentTasks = _tasks.value.toMutableList()
                val index = currentTasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    currentTasks[index] = task
                    _tasks.value = currentTasks
                }
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun updateTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskWithGeofenceUseCase(task)
                // 本地更新状态，避免重新加载
                val currentTasks = _tasks.value.toMutableList()
                val index = currentTasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    currentTasks[index] = task
                    _tasks.value = currentTasks
                }
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskSuspend(task)
        }
    }

    suspend fun deleteTaskSuspend(task: Task): Boolean {
        return try {
            Timber.d("🗑️ 开始删除任务: ${task.title} (ID: ${task.id})")
            
            if (task.location != null && task.latitude != null && task.longitude != null) {
                deleteTaskWithGeofenceUseCase(task)
                Timber.d("任务已删除（带地理围栏）: ${task.title}")
            } else {
                deleteTaskUseCase(task)
                Timber.d("任务已删除: ${task.title}")
            }
            
            // 本地更新状态，避免重新加载
            val currentTasks = _tasks.value.toMutableList()
            currentTasks.removeAll { it.id == task.id }
            _tasks.value = currentTasks
            Timber.d("已更新本地任务列表，当前任务数量: ${currentTasks.size}")
            
            true // 删除成功
        } catch (e: Exception) {
            Timber.e(e, "删除任务失败: ${task.title}")
            // 如果本地更新失败，回退到重新加载
            loadTasks()
            false // 删除失败
        }
    }

    fun deleteTaskWithGeofence(task: Task) {
        viewModelScope.launch {
            try {
                deleteTaskWithGeofenceUseCase(task)
                // 本地更新状态，避免重新加载
                val currentTasks = _tasks.value.toMutableList()
                currentTasks.removeAll { it.id == task.id }
                _tasks.value = currentTasks
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                updateTaskUseCase(updatedTask)
                // 本地更新状态，避免重新加载
                val currentTasks = _tasks.value.toMutableList()
                val index = currentTasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    currentTasks[index] = updatedTask
                    _tasks.value = currentTasks
                }
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
            }
        }
    }

    fun toggleTaskReminder(taskId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val task = getTasksUseCase.getTaskById(taskId)
                task?.let {
                    val updatedTask = it.copy(isReminderEnabled = isEnabled)
                    updateTaskUseCase(updatedTask)
                    // 本地更新状态，避免重新加载
                    val currentTasks = _tasks.value.toMutableList()
                    val index = currentTasks.indexOfFirst { it.id == taskId }
                    if (index != -1) {
                        currentTasks[index] = updatedTask
                        _tasks.value = currentTasks
                    }
                }
            } catch (e: Exception) {
                // 如果本地更新失败，回退到重新加载
                loadTasks()
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
        Timber.d("更新选中位置:")
        Timber.d("  地址: $location")
        Timber.d("  纬度: $latitude")
        Timber.d("  经度: $longitude")
        
        selectedLocation = location
        selectedLatitude = latitude
        selectedLongitude = longitude
    }

    fun updateGeofenceRadius(radius: Float) {
        geofenceRadius = radius
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
        geofenceRadius = 200f
    }

    // 创建并保存任务
    suspend fun saveTask(): Boolean {
        return if (taskTitle.isNotBlank()) {
            Timber.d("开始保存任务:")
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
                longitude = selectedLongitude,
                geofenceRadius = geofenceRadius
            )
            
            Timber.d("创建的任务对象:")
            Timber.d("  title: ${task.title}")
            Timber.d("  description: ${task.description}")
            Timber.d("  dueDate: ${task.dueDate}")
            Timber.d("  dueTime: ${task.dueTime}")
            Timber.d("  isReminderEnabled: ${task.isReminderEnabled}")
            Timber.d("  location: ${task.location}")
            Timber.d("  latitude: ${task.latitude}")
            Timber.d("  longitude: ${task.longitude}")
            Timber.d("  geofenceRadius: ${task.geofenceRadius}")
            
            try {
                // 保存任务并获取生成的ID
                val taskId: Long = if (selectedLocation != null && selectedLatitude != null && selectedLongitude != null) {
                    val id = addTaskWithGeofenceUseCase(task)
                    Timber.d("任务已保存（带地理围栏）: ${task.title}")
                    id
                } else {
                    val id = addTaskUseCase(task)
                    Timber.d("任务已保存: ${task.title}")
                    id
                }
                
                // 本地更新状态，避免重新加载
                val newTask = task.copy(id = taskId)
                val currentTasks = _tasks.value.toMutableList()
                currentTasks.add(newTask)
                _tasks.value = currentTasks
                Timber.d("已更新本地任务列表，当前任务数量: ${currentTasks.size}")
                
                // 如果启用了提醒，调度精确提醒
                if (task.isReminderEnabled) {
                    Timber.d("开始调度任务提醒: taskId=$taskId, title=${task.title}")
                    taskReminderManager.scheduleTaskReminderForTime(
                        taskId = taskId,
                        dueDate = task.dueDate,
                        dueTime = task.dueTime
                    )
                    Timber.d("任务提醒调度完成: ${task.title}")
                } else {
                    Timber.d("任务未启用提醒: ${task.title}")
                }
                
                // 保存后清空表单
                clearTaskForm()
                
                true // 保存成功
            } catch (e: Exception) {
                Timber.e(e, "保存任务失败: ${task.title}")
                // 如果本地更新失败，回退到重新加载
                loadTasks()
                false // 保存失败
            }
        } else {
            false // 标题为空，保存失败
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 释放语音任务管理器资源
        voiceTaskManager.release()
        Timber.d("TaskViewModel已清理，语音任务管理器资源已释放")
    }
}

enum class FilterType {
    ALL, COMPLETED, INCOMPLETE
}
