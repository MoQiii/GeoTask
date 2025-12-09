package com.syj.geotask

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.syj.geotask.data.service.IGeofenceManager
import com.syj.geotask.data.service.TaskReminderManager
import com.syj.geotask.data.work.TaskReminderWorker
import com.syj.geotask.domain.usecase.GetTasksUseCase
import com.syj.geotask.utils.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GeoTaskApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var getTasksUseCase: GetTasksUseCase
    
    @Inject
    lateinit var taskReminderManager: TaskReminderManager
    
    @Inject
    lateinit var geofenceManager: IGeofenceManager
    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory
    
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 初始化Timber日志框架
        initializeTimber()
        
        // 重新调度所有任务提醒
        rescheduleAllTaskReminders()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()


    private fun initializeTimber() {
        // 同时输出到控制台和文件
        Timber.plant(Timber.DebugTree())
        
        // 文件输出
        val fileLoggingTree = FileLoggingTree(this)
        Timber.plant(fileLoggingTree)
        
        Timber.d("Timber初始化完成 - 控制台+文件日志")
    }
    
    private fun rescheduleAllTaskReminders() {
        applicationScope.launch {
            try {
                // 获取所有任务
                val tasks = getTasksUseCase.getAllTasks().first()
                // 重新调度所有启用提醒的任务
                taskReminderManager.rescheduleAllTaskReminders(tasks)
                Timber.d("应用启动时重新调度了 ${tasks.count { it.isReminderEnabled && !it.isCompleted }} 个任务提醒")
                
                // 初始化地理围栏服务
                initializeGeofenceService(tasks)
                
                // 初始化定期任务提醒工作
                initializePeriodicTaskReminder()
            } catch (e: Exception) {
                Timber.e(e, "重新调度任务提醒失败")
            }
        }
    }
    
    private fun initializeGeofenceService(tasks: List<com.syj.geotask.domain.model.Task>) {
        try {
            // 为所有有位置的任务重新添加地理围栏
            val locationTasks = tasks.filter { 
                it.latitude != null && 
                it.longitude != null && 
                it.location != null && 
                !it.isCompleted 
            }
            
            Timber.d("开始初始化地理围栏服务，共 ${locationTasks.size} 个任务需要地理围栏")
            
            locationTasks.forEach { task ->
                applicationScope.launch {
                    try {
                        val success = geofenceManager.addGeofenceForTask(task)
                        if (success) {
                            Timber.d("地理围栏添加成功: taskId=${task.id}, location=${task.location}")
                        } else {
                            Timber.w("地理围栏添加失败: taskId=${task.id}, location=${task.location}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "添加地理围栏时发生异常: taskId=${task.id}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "初始化地理围栏服务失败")
        }
    }
    
    private fun initializePeriodicTaskReminder() {
        try {
            // 创建约束条件
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()
            
            // 创建定期工作请求 - 每15分钟检查一次任务提醒
            val periodicWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10_000, // 10秒
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // 调度工作
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                TaskReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
            
            Timber.d("定期任务提醒工作已调度")
        } catch (e: Exception) {
            Timber.e(e, "定期任务提醒工作调度失败")
        }
    }
}
