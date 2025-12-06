package com.syj.geotask.data.service

import android.content.Context
import androidx.work.*
import com.syj.geotask.data.work.SingleTaskReminderWorker
import com.syj.geotask.data.work.TaskReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 为单个任务调度精确提醒
     * @param taskId 任务ID
     * @param delayMillis 延迟时间（毫秒）
     */
    fun scheduleTaskReminder(taskId: Long, delayMillis: Long) {
        try {
            Timber.d("开始调度任务提醒: taskId=$taskId, 原始延迟=${delayMillis}ms")
            
            // 计算延迟时间，确保不小于1分钟（WorkManager最小限制）
            val actualDelay = maxOf(delayMillis, 60_000L) // 最小1分钟
            
            Timber.d("调整后延迟时间: ${actualDelay}ms (${actualDelay / 1000}秒)")
            
            val data = Data.Builder()
                .putLong(SingleTaskReminderWorker.TASK_ID_KEY, taskId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SingleTaskReminderWorker>()
                .setInitialDelay(actualDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(SingleTaskReminderWorker.WORK_NAME_PREFIX + taskId)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SingleTaskReminderWorker.getWorkName(taskId),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Timber.d("成功调度任务 $taskId 的提醒，延迟 ${actualDelay}ms (${actualDelay / 1000}秒)")
            Timber.d("WorkRequest ID: ${workRequest.id}")
            Timber.d("WorkRequest Tag: ${SingleTaskReminderWorker.WORK_NAME_PREFIX + taskId}")
        } catch (e: Exception) {
            Timber.e(e, "调度任务提醒失败: taskId=$taskId, delay=$delayMillis")
        }
    }

    /**
     * 取消指定任务的提醒
     */
    fun cancelTaskReminder(taskId: Long) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(
                SingleTaskReminderWorker.getWorkName(taskId)
            )
            Timber.d("已取消任务 $taskId 的提醒")
        } catch (e: Exception) {
            Timber.e(e, "取消任务提醒失败: taskId=$taskId")
        }
    }

    /**
     * 为任务计算并调度提醒
     * @param taskId 任务ID
     * @param dueDate 任务日期（时间戳）
     * @param dueTime 任务时间（时间戳）
     */
    fun scheduleTaskReminderForTime(taskId: Long, dueDate: Long, dueTime: Long) {
        try {
            // 合并日期和时间
            val taskDateTime = combineDateAndTime(dueDate, dueTime)
            val currentTime = System.currentTimeMillis()
            val delayMillis = taskDateTime - currentTime

            if (delayMillis <= 0) {
                Timber.w("任务时间已过，不调度提醒: taskId=$taskId, taskTime=$taskDateTime, currentTime=$currentTime")
                return
            }

            scheduleTaskReminder(taskId, delayMillis)
        } catch (e: Exception) {
            Timber.e(e, "计算任务提醒时间失败: taskId=$taskId")
        }
    }

    /**
     * 重新调度所有启用提醒的任务
     * 这个方法可以在应用启动时调用，确保所有任务都有正确的提醒
     */
    fun rescheduleAllTaskReminders(tasks: List<com.syj.geotask.domain.model.Task>) {
        try {
            // 首先取消所有现有的单次提醒
            cancelAllTaskReminders()

            // 为每个启用提醒的任务重新调度
            tasks.forEach { task ->
                if (task.isReminderEnabled && !task.isCompleted) {
                    scheduleTaskReminderForTime(task.id, task.dueDate, task.dueTime)
                }
            }

            Timber.d("已重新调度 ${tasks.count { it.isReminderEnabled && !it.isCompleted }} 个任务提醒")
        } catch (e: Exception) {
            Timber.e(e, "重新调度任务提醒失败")
        }
    }

    /**
     * 取消所有任务提醒
     */
    private fun cancelAllTaskReminders() {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag(
                SingleTaskReminderWorker.WORK_NAME_PREFIX
            )
            Timber.d("已取消所有任务提醒")
        } catch (e: Exception) {
            Timber.e(e, "取消所有任务提醒失败")
        }
    }

    /**
     * 合并日期和时间
     */
    private fun combineDateAndTime(dateMillis: Long, timeMillis: Long): Long {
        val dateCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val timeCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }

        dateCalendar.set(java.util.Calendar.HOUR_OF_DAY, timeCalendar.get(java.util.Calendar.HOUR_OF_DAY))
        dateCalendar.set(java.util.Calendar.MINUTE, timeCalendar.get(java.util.Calendar.MINUTE))
        dateCalendar.set(java.util.Calendar.SECOND, 0)
        dateCalendar.set(java.util.Calendar.MILLISECOND, 0)

        return dateCalendar.timeInMillis
    }

    /**
     * 获取任务提醒状态
     */
    fun getTaskReminderStatus(taskId: Long): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosByTag(SingleTaskReminderWorker.WORK_NAME_PREFIX + taskId)
                .get()
            workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        } catch (e: Exception) {
            Timber.e(e, "获取任务提醒状态失败: taskId=$taskId")
            false
        }
    }
}
