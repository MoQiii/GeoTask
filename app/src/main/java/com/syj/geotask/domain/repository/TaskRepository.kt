package com.syj.geotask.domain.repository

import com.syj.geotask.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(id: Long)
    suspend fun updateTaskCompletionStatus(id: Long, isCompleted: Boolean)
    suspend fun updateTaskReminderStatus(id: Long, isEnabled: Boolean)
    
    // 地理围栏相关方法
    suspend fun insertTaskWithGeofence(task: Task): Long
    suspend fun updateTaskWithGeofence(task: Task)
    suspend fun deleteTaskWithGeofence(task: Task)
    suspend fun deleteTaskByIdWithGeofence(id: Long)
}
