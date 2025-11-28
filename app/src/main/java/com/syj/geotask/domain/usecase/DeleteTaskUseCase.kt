package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.deleteTask(task)
    }
    
    suspend fun deleteById(id: Long) {
        repository.deleteTaskById(id)
    }
}
