package com.syj.geotask.domain.usecase

import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.repository.TaskRepository
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Long {
        return repository.insertTask(task)
    }
}
