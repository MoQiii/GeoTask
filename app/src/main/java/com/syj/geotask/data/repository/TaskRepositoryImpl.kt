package com.syj.geotask.data.repository

import android.content.Context
import com.syj.geotask.data.datasource.local.TaskDao
import com.syj.geotask.data.service.GeofenceManager
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val geofenceManager: GeofenceManager,
    private val context: Context
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
    }
    
    override fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>> {
        return taskDao.getTasksByCompletionStatus(isCompleted)
    }
    
    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query)
    }
    
    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }
    
    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    override suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }
    
    override suspend fun updateTaskCompletionStatus(id: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompletionStatus(id, isCompleted)
    }
    
    override suspend fun updateTaskReminderStatus(id: Long, isEnabled: Boolean) {
        taskDao.updateTaskReminderStatus(id, isEnabled)
    }
    
    // 地理围栏相关方法实现
    override suspend fun insertTaskWithGeofence(task: Task): Long {
        val taskId = taskDao.insertTask(task)
        
        // 如果任务有位置信息，创建地理围栏
        if (task.latitude != null && task.longitude != null && task.location != null) {
            try {
                val taskWithId = task.copy(id = taskId)
                geofenceManager.addGeofenceForTask(taskWithId)
            } catch (e: Exception) {
                // 地理围栏创建失败不影响任务创建
                // 可以记录日志或发送错误报告
            }
        }
        
        return taskId
    }
    
    override suspend fun updateTaskWithGeofence(task: Task) {
        // 先移除旧的地理围栏
        try {
            geofenceManager.removeGeofenceForTask(task.id)
        } catch (e: Exception) {
            // 移除失败不影响更新
        }
        
        // 更新任务
        taskDao.updateTask(task)
        
        // 如果任务有位置信息，创建新的地理围栏
        if (task.latitude != null && task.longitude != null && task.location != null) {
            try {
                geofenceManager.addGeofenceForTask(task)
            } catch (e: Exception) {
                // 地理围栏创建失败不影响任务更新
            }
        }
    }
    
    override suspend fun deleteTaskWithGeofence(task: Task) {
        // 先移除地理围栏
        try {
            geofenceManager.removeGeofenceForTask(task.id)
        } catch (e: Exception) {
            // 移除失败不影响删除
        }
        
        // 删除任务
        taskDao.deleteTask(task)
    }
    
    override suspend fun deleteTaskByIdWithGeofence(id: Long) {
        // 先获取任务信息
        val task = taskDao.getTaskById(id)
        
        // 移除地理围栏
        if (task != null) {
            try {
                geofenceManager.removeGeofenceForTask(task.id)
            } catch (e: Exception) {
                // 移除失败不影响删除
            }
        }
        
        // 删除任务
        taskDao.deleteTaskById(id)
    }
}
