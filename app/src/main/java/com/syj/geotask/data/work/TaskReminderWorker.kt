package com.syj.geotask.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.usecase.GetTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTasksUseCase: GetTasksUseCase,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 获取所有启用了提醒的任务
            val tasks = getTasksUseCase.getAllTasks()
            val currentTime = System.currentTimeMillis()
            val today = Calendar.getInstance()
            
            tasks.collect { taskList ->
                taskList.forEach { task ->
                    if (task.isReminderEnabled && !task.isCompleted) {
                        // 检查是否是今天或即将到期的任务
                        val taskCalendar = Calendar.getInstance().apply {
                            timeInMillis = task.dueDate
                        }
                        
                        if (isTaskDueSoon(taskCalendar, today)) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val taskTime = timeFormat.format(Date(task.dueTime))
                            
                            notificationService.showTaskReminderNotification(
                                taskId = task.id,
                                taskTitle = task.title,
                                taskDescription = task.description,
                                taskTime = taskTime
                            )
                        }
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun isTaskDueSoon(taskDate: Calendar, today: Calendar): Boolean {
        // 检查任务是否在今天或明天
        val taskDay = taskDate.get(Calendar.DAY_OF_YEAR)
        val todayDay = today.get(Calendar.DAY_OF_YEAR)
        val tomorrowDay = todayDay + 1
        
        return taskDay == todayDay || taskDay == tomorrowDay
    }

    companion object {
        const val WORK_NAME = "TaskReminderWorker"
    }
}
