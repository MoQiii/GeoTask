package com.syj.geotask.data.repository

import android.content.Context
import com.syj.geotask.data.service.IGeofenceManager
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import org.openapitools.client.apis.TaskControllerApi
import timber.log.Timber
import org.openapitools.client.models.Task as RemoteTask
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskControllerApi: TaskControllerApi,
    private val geofenceManager: IGeofenceManager,
    private val context: Context
) : TaskRepository {

    // 转换函数：远程Task -> 域Task
    private fun RemoteTask.toDomainTask(): Task {
        return Task(
            id = this.id ?: 0L,
            title = this.title ?: "",
            description = this.description ?: "",
            dueDate = this.dueDate ?: 0L,
            dueTime = this.dueTime ?: 0L,
            isCompleted = this.isCompleted ?: false,
            isReminderEnabled = this.isReminderEnabled ?: false,
            location = this.location,
            latitude = this.latitude,
            longitude = this.longitude,
            geofenceRadius = this.geofenceRadius ?: 200f,
            createdAt = this.createdAt ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt ?: System.currentTimeMillis()
        )
    }

    // 转换函数：域Task -> 远程Task
    private fun Task.toRemoteTask(): RemoteTask {
        return RemoteTask(
            id = if (this.id == 0L) null else this.id,
            title = this.title,
            description = this.description,
            dueDate = this.dueDate,
            dueTime = this.dueTime,
            isCompleted = this.isCompleted,
            isReminderEnabled = this.isReminderEnabled,
            location = this.location,
            latitude = this.latitude,
            longitude = this.longitude,
            geofenceRadius = this.geofenceRadius,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
    
//    override fun getAllTasks(): Flow<List<Task>> {
//        return flow {
//            try {
//                Timber.d("开始从远程API获取所有任务")
//                val tasks = withContext(Dispatchers.IO) {
//                    taskControllerApi.getAllTasks()
//                }
//                Timber.d("API返回了 ${tasks.size} 个任务")
//                val domainTasks = tasks.map { it.toDomainTask() }
//                Timber.d("转换后的任务列表: ${domainTasks.map { "${it.id}:${it.title}" }}")
//                emit(domainTasks)
//                Timber.d("已发送任务列表到Flow")
//            } catch (e: Exception) {
//                Timber.e(e, "获取任务列表失败")
//                emit(emptyList())
//            }
//        }
//    }
    override fun getAllTasks(): Flow<List<Task>> {
        return flow {
            Timber.d("开始从远程API获取所有任务")

            val tasks = withContext(Dispatchers.IO) {
                taskControllerApi.getAllTasks()
            }

            Timber.d("API返回了 ${tasks.size} 个任务")

            val domainTasks = tasks.map { it.toDomainTask() }
            Timber.d("转换后的任务列表: ${domainTasks.map { "${it.id}:${it.title}" }}")

            emit(domainTasks)
            Timber.d("已发送任务列表到Flow")
        }.catch { e ->
            Timber.e(e, "获取任务列表失败")
            emit(emptyList())    // ✔️ 这里是被允许的，不会再报 Flow 异常
        }
    }

    override fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>> {
        return flow {
            try {
                val tasks = withContext(Dispatchers.IO) {
                    taskControllerApi.getTasksByCompleted(isCompleted)
                }
                emit(tasks.map { it.toDomainTask() })
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }
    
    override fun searchTasks(query: String): Flow<List<Task>> {
        return flow {
            try {
                val tasks = withContext(Dispatchers.IO) {
                    taskControllerApi.searchTasksByTitle(query)
                }
                emit(tasks.map { it.toDomainTask() })
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }
    
    override suspend fun getTaskById(id: Long): Task? {
        return try {
            withContext(Dispatchers.IO) {
                taskControllerApi.getTaskById(id).toDomainTask()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun insertTask(task: Task): Long {
        return try {
            withContext(Dispatchers.IO) {
                val remoteTask = taskControllerApi.createTask(task.toRemoteTask())
                remoteTask.id ?: -1L
            }
        } catch (e: Exception) {
            -1L // 表示创建失败
        }
    }
    
    override suspend fun updateTask(task: Task) {
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.updateTask(task.id, task.toRemoteTask())
            }
        } catch (e: Exception) {
            // 处理更新失败
        }
    }
    
    override suspend fun deleteTask(task: Task) {
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.deleteTask(task.id)
            }
        } catch (e: Exception) {
            // 处理删除失败
        }
    }
    
    override suspend fun deleteTaskById(id: Long) {
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.deleteTask(id)
            }
        } catch (e: Exception) {
            // 处理删除失败
        }
    }
    
    override suspend fun updateTaskCompletionStatus(id: Long, isCompleted: Boolean) {
        try {
            withContext(Dispatchers.IO) {
                if (isCompleted) {
                    taskControllerApi.markTaskAsCompleted(id)
                } else {
                    taskControllerApi.markTaskAsUncompleted(id)
                }
            }
        } catch (e: Exception) {
            // 处理更新失败
        }
    }
    
    override suspend fun updateTaskReminderStatus(id: Long, isEnabled: Boolean) {
        try {
            withContext(Dispatchers.IO) {
                if (isEnabled) {
                    taskControllerApi.enableTaskReminder(id)
                } else {
                    taskControllerApi.disableTaskReminder(id)
                }
            }
        } catch (e: Exception) {
            // 处理更新失败
        }
    }
    
    // 地理围栏相关方法实现
    override suspend fun insertTaskWithGeofence(task: Task): Long {
        val taskId = try {
            withContext(Dispatchers.IO) {
                val remoteTask = taskControllerApi.createTask(task.toRemoteTask())
                remoteTask.id ?: -1L
            }
        } catch (e: Exception) {
            -1L // 表示创建失败
        }
        
        // 如果任务创建成功且有位置信息，创建地理围栏
        if (taskId > 0 && task.latitude != null && task.longitude != null && task.location != null) {
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
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.updateTask(task.id, task.toRemoteTask())
            }
        } catch (e: Exception) {
            // 处理更新失败
            return
        }
        
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
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.deleteTask(task.id)
            }
        } catch (e: Exception) {
            // 处理删除失败
        }
    }
    
    override suspend fun deleteTaskByIdWithGeofence(id: Long) {
        // 先获取任务信息
        val task = try {
            withContext(Dispatchers.IO) {
                taskControllerApi.getTaskById(id).toDomainTask()
            }
        } catch (e: Exception) {
            null
        }
        
        // 移除地理围栏
        if (task != null) {
            try {
                geofenceManager.removeGeofenceForTask(task.id)
            } catch (e: Exception) {
                // 移除失败不影响删除
            }
        }
        
        // 删除任务
        try {
            withContext(Dispatchers.IO) {
                taskControllerApi.deleteTask(id)
            }
        } catch (e: Exception) {
            // 处理删除失败
        }
    }
}
