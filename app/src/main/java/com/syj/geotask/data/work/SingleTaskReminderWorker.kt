package com.syj.geotask.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.domain.usecase.GetTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class SingleTaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getTasksUseCase: GetTasksUseCase,
    private val notificationService: NotificationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val taskId = inputData.getLong(TASK_ID_KEY, -1L)
            if (taskId == -1L) {
                Timber.e("SingleTaskReminderWorker: 无效的任务ID")
                return Result.failure()
            }

            Timber.d("SingleTaskReminderWorker开始处理任务 $taskId 的提醒")
            Timber.d("当前时间: ${Date()}")

            // 获取指定任务
            val task = getTasksUseCase.getTaskById(taskId)
            if (task == null) {
                Timber.w("SingleTaskReminderWorker: 找不到任务 $taskId")
                return Result.failure()
            }

            // 检查任务是否仍然需要提醒
            if (!task.isReminderEnabled || task.isCompleted) {
                Timber.d("SingleTaskReminderWorker: 任务 $taskId 不需要提醒（已禁用或已完成）")
                return Result.success()
            }

            notificationService.showTaskReminderNotification(task)

            Timber.d("SingleTaskReminderWorker完成 - 任务 $taskId 提醒已发送")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SingleTaskReminderWorker执行失败")
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PREFIX = "SingleTaskReminder_"
        const val TASK_ID_KEY = "task_id"
        
        fun getWorkName(taskId: Long): String = "$WORK_NAME_PREFIX$taskId"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            workerParams: WorkerParameters
        ): SingleTaskReminderWorker
    }
}
