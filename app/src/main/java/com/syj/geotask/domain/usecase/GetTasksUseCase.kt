package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    fun getAllTasks(): Flow<List<Task>> {
        return repository.getAllTasks()
    }
    
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>> {
        return repository.getTasksByCompletionStatus(isCompleted)
    }
    
    fun searchTasks(query: String): Flow<List<Task>> {
        return repository.searchTasks(query)
    }
    
    suspend fun getTaskById(id: Long): Task? {
        return repository.getTaskById(id)
    }
}
