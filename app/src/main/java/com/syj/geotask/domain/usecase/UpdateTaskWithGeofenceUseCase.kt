package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskWithGeofenceUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.updateTaskWithGeofence(task)
    }
}
