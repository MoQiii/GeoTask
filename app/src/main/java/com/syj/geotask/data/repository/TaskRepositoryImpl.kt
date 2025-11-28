package com.syj.geotask.data.repository

import com.syj.geotask.data.datasource.local.TaskDao
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
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
}
