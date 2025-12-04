package com.syj.geotask.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.syj.geotask.MainActivity
import com.syj.geotask.R
import com.syj.geotask.domain.model.Task
import com.syj.geotask.utils.PermissionUtils
import timber.log.Timber
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationService @Inject constructor(private val context: Context) {

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminder_channel"
        const val TASK_REMINDER_CHANNEL_NAME = "任务提醒"
        const val TASK_REMINDER_CHANNEL_DESCRIPTION = "任务到期提醒通知"
    }

    // 高德地图定位服务实例
    private val aMapLocationService: AMapLocationService by lazy {
        AMapLocationService(context)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TASK_REMINDER_CHANNEL_ID,
                TASK_REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = TASK_REMINDER_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminderNotification(
        taskId: Long,
        taskTitle: String,
        taskDescription: String,
        taskTime: String
    ) {
        showTaskReminderNotification(taskId, taskTitle, taskDescription, taskTime, null, null, 0f)
    }
    
    /**
     * 根据任务对象显示任务提醒通知
     * 这个方法会根据任务的实际数据来传递位置信息
     */
    fun showTaskReminderNotification(task: Task) {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val taskTime = timeFormat.format(java.util.Date(task.dueTime))
        val dateFormat = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        val taskDate = dateFormat.format(java.util.Date(task.dueDate))
        
        showTaskReminderNotification(
            taskId = task.id,
            taskTitle = task.title,
            taskDescription = task.description,
            taskTime = "$taskDate $taskTime",
            taskLatitude = task.latitude,
            taskLongitude = task.longitude,
            geofenceRadius = task.geofenceRadius
        )
    }
    
    fun showTaskReminderNotification(
        taskId: Long,
        taskTitle: String,
        taskDescription: String,
        taskTime: String,
        taskLatitude: Double?,
        taskLongitude: Double?,
        geofenceRadius: Float
    ) {
        Timber.d("🔔 准备显示任务提醒通知: taskId=$taskId, title=$taskTitle")
        
        // 检查通知权限
        if (!PermissionUtils.hasNotificationPermission(context)) {
            Timber.e("❌ 通知权限未授予，无法显示任务提醒通知")
            return
        }

        Timber.d("✅ 通知权限检查通过")
        
        // 如果提供了位置信息，进行位置验证
        if (taskLatitude != null && taskLongitude != null && geofenceRadius > 0) {
            val currentLocation = getCurrentLocation()
            if (currentLocation != null) {
                val targetLocation = Location("task").apply {
                    latitude = taskLatitude
                    longitude = taskLongitude
                }
                val distance = currentLocation.distanceTo(targetLocation)
                
                Timber.d("📍 位置验证详情:")
                Timber.d("  taskId=$taskId")
                Timber.d("  当前位置: lat=${currentLocation.latitude}, lng=${currentLocation.longitude}")
                Timber.d("  目标位置: lat=$taskLatitude, lng=$taskLongitude")
                Timber.d("  计算距离: ${distance}m")
                Timber.d("  围栏半径: ${geofenceRadius}m")
                
                // 检查经纬度是否在合理范围内
                if (taskLatitude < -90 || taskLatitude > 90 || taskLongitude < -180 || taskLongitude > 180) {
                    Timber.e("❌ 任务经纬度数据异常: lat=$taskLatitude, lng=$taskLongitude")
                    return
                }
                
                if (currentLocation.latitude < -90 || currentLocation.latitude > 90 || 
                    currentLocation.longitude < -180 || currentLocation.longitude > 180) {
                    Timber.e("❌ 当前位置经纬度数据异常: lat=${currentLocation.latitude}, lng=${currentLocation.longitude}")
                    return
                }
                
                // 如果距离超过1000km，可能是数据问题
                if (distance > 1000000) {
                    Timber.w("⚠️ 距离异常过大(${distance}m)，可能是经纬度数据问题，跳过位置验证")
                    // 不return，继续发送通知，但记录警告
                } else if (distance > geofenceRadius) {
                    Timber.w("❌ 位置验证失败，距离过远不发送通知: taskId=$taskId, 距离=${distance}m, 半径=${geofenceRadius}m")
                    return
                } else {
                    Timber.d("✅ 位置验证通过，发送通知: taskId=$taskId, 距离=${distance}m")
                }
            } else {
                Timber.w("⚠️ 无法获取当前位置，跳过位置验证: taskId=$taskId")
            }
        }
        
        try {
            // 创建点击通知后的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("task_id", taskId)
                putExtra("navigate_to", "task_detail")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("任务提醒")
                .setContentText("$taskTime $taskTitle")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$taskTime $taskTitle\n$taskDescription")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(taskId.toInt(), notification)
                Timber.d("✅ 任务提醒通知已成功发送: taskId=$taskId, title=$taskTitle")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "❌ 显示任务提醒通知时发生SecurityException，权限可能被拒绝")
        } catch (e: Exception) {
            Timber.e(e, "❌ 显示任务提醒通知时发生未知错误")
        }
    }

    fun showLocationReminderNotification(
        taskId: Long,
        taskTitle: String,
        location: String
    ) {
        // 检查通知权限
        if (!PermissionUtils.hasNotificationPermission(context)) {
            Timber.w("通知权限未授予，无法显示位置提醒通知")
            return
        }

        try {
            // 创建点击通知后的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("task_id", taskId)
                putExtra("navigate_to", "task_detail")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("位置提醒")
                .setContentText("您已到达 $location")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("您已到达 $location\n任务: $taskTitle")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(taskId.toInt() + 10000, notification) // 使用不同的ID避免冲突
            }
        } catch (e: SecurityException) {
            Timber.w(e, "显示位置提醒通知时发生SecurityException，权限可能被拒绝")
        }
    }

    fun cancelNotification(taskId: Long) {
        // 检查通知权限
        if (!PermissionUtils.hasNotificationPermission(context)) {
            Timber.w("通知权限未授予，无法取消通知")
            return
        }

        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(taskId.toInt())
                cancel(taskId.toInt() + 10000) // 也取消位置提醒通知
            }
        } catch (e: SecurityException) {
            Timber.w(e, "取消通知时发生SecurityException，权限可能被拒绝")
        }
    }

    /**
     * 获取当前位置（使用高德地图定位服务）
     */
    private fun getCurrentLocation(): Location? {
        return try {

            var locationResult: Location? = null
            var locationCompleted = false
            
            // 在 IO 线程中执行定位
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val location = aMapLocationService.getCurrentLocation()
                    locationResult = location
                    locationCompleted = true
                } catch (e: Exception) {
                    Timber.e(e, "❌ 高德地图定位失败")
                    locationCompleted = true
                }
            }
            
            // 等待定位完成（最多等待10秒）
            val startTime = System.currentTimeMillis()
            while (!locationCompleted && System.currentTimeMillis() - startTime < 10000) {
                Thread.sleep(100)
            }
            
            if (locationResult != null) {
                Timber.d("✅ 高德地图定位成功: lat=${locationResult!!.latitude}, lng=${locationResult!!.longitude}, accuracy=${locationResult!!.accuracy}m")
                return locationResult
            }
            
            // 如果高德定位失败，回退到原生定位
            Timber.w("⚠️ 高德地图定位失败，回退到原生定位")
            getNativeLocation()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ 获取当前位置时发生异常，回退到原生定位")
            getNativeLocation()
        }
    }
    
    /**
     * 原生定位方法（作为高德定位的备选方案）
     */
    private fun getNativeLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // 尝试获取GPS位置
            val gpsLocation = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
            
            // 如果GPS位置不可用，尝试网络位置
            val networkLocation = try {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
            
            // 选择最新且最准确的位置
            val result = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
            
            if (result != null) {
                Timber.d("✅ 原生定位成功: lat=${result.latitude}, lng=${result.longitude}, accuracy=${result.accuracy}m, provider=${result.provider}")
            } else {
                Timber.w("❌ 原生定位也失败")
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "❌ 原生定位时发生异常")
            null
        }
    }

    /**
     * 测试通知功能
     * 用于立即测试通知是否正常工作
     */
    fun showTestNotification() {
        Timber.d("🧪 开始测试通知功能")
        
        // 检查通知权限
        if (!PermissionUtils.hasNotificationPermission(context)) {
            Timber.e("❌ 测试失败：通知权限未授予")
            return
        }

        Timber.d("✅ 测试：通知权限检查通过")
        
        try {
            // 创建点击通知后的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("test_notification", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                99999, // 使用特殊的测试ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("测试通知")
                .setContentText("这是一条测试通知，如果您看到这条消息，说明通知功能正常")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("这是一条测试通知\n如果您看到这条消息，说明通知功能正常\n点击此通知将打开应用")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(99999, notification)
                Timber.d("✅ 测试通知已成功发送")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "❌ 测试失败：显示通知时发生SecurityException，权限可能被拒绝")
        } catch (e: Exception) {
            Timber.e(e, "❌ 测试失败：显示通知时发生未知错误")
        }
    }

    /**
     * 测试高德地图定位功能
     * 用于验证高德地图定位服务是否正常工作
     */
    fun testAMapLocation() {
        Timber.d("🧪 开始测试高德地图定位功能")
        
        // 检查位置权限
        if (!PermissionUtils.hasLocationPermission(context)) {
            Timber.e("❌ 测试失败：位置权限未授予")
            return
        }

        Timber.d("✅ 测试：位置权限检查通过")
        
        // 测试高德地图定位服务
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 测试获取缓存位置
                val cachedLocation = aMapLocationService.getLastKnownLocation()
                if (cachedLocation != null) {
                    Timber.d("✅ 高德地图缓存位置测试成功: lat=${cachedLocation.latitude}, lng=${cachedLocation.longitude}, accuracy=${cachedLocation.accuracy}m")
                } else {
                    Timber.d("⚠️ 高德地图缓存位置为空，测试实时定位")
                }
                
                // 测试实时定位
                val currentLocation = aMapLocationService.getCurrentLocation()
                if (currentLocation != null) {
                    Timber.d("✅ 高德地图实时定位测试成功: lat=${currentLocation.latitude}, lng=${currentLocation.longitude}, accuracy=${currentLocation.accuracy}m, provider=${currentLocation.provider}")
                    
                    // 发送定位成功通知
                    showLocationTestNotification(currentLocation, true)
                } else {
                    Timber.e("❌ 高德地图实时定位测试失败")
                    
                    // 发送定位失败通知
                    showLocationTestNotification(null, false)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "❌ 高德地图定位测试时发生异常")
                showLocationTestNotification(null, false)
            }
        }
    }
    
    /**
     * 显示定位测试结果通知
     */
    private fun showLocationTestNotification(location: Location?, success: Boolean) {
        try {
            val title = if (success) "高德定位测试成功" else "高德定位测试失败"
            val content = if (success) {
                "定位成功: lat=${location?.latitude}, lng=${location?.longitude}, 精度=${location?.accuracy}m"
            } else {
                "定位失败，请检查权限和网络连接"
            }
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("location_test", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                88888, // 使用特殊的测试ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(content)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(88888, notification)
                Timber.d("✅ 定位测试通知已发送: $title")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 发送定位测试通知时发生异常")
        }
    }
}
