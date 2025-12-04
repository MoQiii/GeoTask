package com.syj.geotask.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.usecase.GetTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTasksUseCase: GetTasksUseCase,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("TaskReminderWorker开始检查任务提醒")
            
            // 获取所有启用了提醒的任务
            val taskList = getTasksUseCase.getAllTasks().first()
            val currentTime = System.currentTimeMillis()
            val currentCalendar = Calendar.getInstance()
            
            var notificationSent = false
            
            taskList.forEach { task ->
                if (task.isReminderEnabled && !task.isCompleted) {
                    // 合并任务的日期和时间
                    val taskDateTime = Calendar.getInstance().apply {
                        timeInMillis = task.dueDate
                        set(Calendar.HOUR_OF_DAY, getHourFromTime(task.dueTime))
                        set(Calendar.MINUTE, getMinuteFromTime(task.dueTime))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    // 检查任务是否在接下来的15分钟内到期
                    if (isTaskDueSoon(taskDateTime, currentCalendar)) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val taskTime = timeFormat.format(Date(task.dueTime))
                        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                        val taskDate = dateFormat.format(Date(task.dueDate))
                        
                        Timber.d("发送任务提醒: ${task.title}, 时间: $taskDate $taskTime")
                        
                        notificationService.showTaskReminderNotification(task)
                        notificationSent = true
                    }
                }
            }
            
            if (notificationSent) {
                Timber.d("TaskReminderWorker完成 - 已发送提醒通知")
            } else {
                Timber.d("TaskReminderWorker完成 - 无需发送提醒")
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "TaskReminderWorker执行失败")
            Result.failure()
        }
    }

    private fun isTaskDueSoon(taskDateTime: Calendar, currentCalendar: Calendar): Boolean {
        val taskTime = taskDateTime.timeInMillis
        val currentTime = currentCalendar.timeInMillis
        
        // 检查任务时间是否在当前时间之后的15分钟内
        val timeDiff = taskTime - currentTime
        val isDue = timeDiff > 0 && timeDiff <= 15 * 60 * 1000 // 15分钟内
        
        Timber.d("任务时间检查: 任务时间=$taskTime, 当前时间=$currentTime, 时间差=${timeDiff}ms, 是否到期=$isDue")
        
        return isDue
    }
    
    private fun getHourFromTime(timeInMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    private fun getMinuteFromTime(timeInMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return calendar.get(Calendar.MINUTE)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            workerParams: WorkerParameters
        ): TaskReminderWorker
    }

    companion object {
        const val WORK_NAME = "TaskReminderWorker"
    }
}
