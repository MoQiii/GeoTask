package com.syj.geotask.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import com.amap.api.fence.GeoFence
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.syj.geotask.data.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationService = NotificationService(context)
        
        Timber.d("地理围栏广播接收器收到意图: ${intent.action}")
        
        // 首先尝试处理高德地图的地理围栏触发
        if (handleAMapGeofence(context, intent, notificationService)) {
            return
        }
        
        // 如果不是高德地图的触发，尝试处理Google Play Services的地理围栏
        handleGoogleGeofence(context, intent, notificationService)
    }
    
    /**
     * 处理高德地图地理围栏触发
     */
    private fun handleAMapGeofence(context: Context, intent: Intent, notificationService: NotificationService): Boolean {
        try {
            // 高德地图地理围栏触发的Action格式
            val action = intent.action
            if (action?.startsWith("com.syj.geotask.GEOFENCE_TRANSITION_") == true) {
                val geofenceId = intent.getStringExtra("geofence_id")
                val geofenceStatus = intent.getIntExtra("geofence_status", -1)
                val source = intent.getStringExtra("source")
                
                Timber.d("高德地图地理围栏触发: id=$geofenceId, status=$geofenceStatus, source=$source")
                
                // 检查是否是进入围栏（状态为1表示进入）
                if (geofenceStatus == GeoFence.STATUS_IN && geofenceId != null) {
                    val taskId = geofenceId.toLongOrNull()
                    
                    if (taskId != null) {
                        // 如果是来自高德地图的直接触发，需要获取任务信息
                        if (source == "amap") {
                            // 这里需要从数据库或其他方式获取任务信息
                            // 由于在BroadcastReceiver中无法直接使用依赖注入，我们简化处理
                            // 直接使用geofenceId作为taskId，并尝试从Intent中获取信息
                            validateLocationAndNotifyWithTaskId(context, geofenceId, taskId, notificationService)
                        } else {
                            // 如果Intent中已经包含任务信息，直接验证
                            validateLocationAndNotify(context, intent, taskId, notificationService)
                        }
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "处理高德地图地理围栏时发生异常")
        }
        
        return false
    }
    
    /**
     * 处理Google Play Services地理围栏触发
     */
    private fun handleGoogleGeofence(context: Context, intent: Intent, notificationService: NotificationService) {
        try {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            
            if (geofencingEvent == null || geofencingEvent.hasError()) {
                Timber.w("Google地理围栏事件无效或存在错误")
                return
            }

            // 获取触发的地理围栏转换
            val geofenceTransition = geofencingEvent.geofenceTransition
            
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // 获取触发的地理围栏列表
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                
                triggeringGeofences?.forEach { geofence ->
                    val taskId = geofence.requestId.toLongOrNull()
                    
                    if (taskId != null) {
                        Timber.d("Google地理围栏触发: taskId=$taskId")
                        
                        // 验证实际位置距离
                        validateLocationAndNotify(context, intent, taskId, notificationService)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "处理Google地理围栏时发生异常")
        }
    }
    
    /**
     * 验证实际位置距离并发送通知
     */
    private fun validateLocationAndNotify(context: Context, intent: Intent, taskId: Long, notificationService: NotificationService) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 从Intent中获取任务信息（简化方案，避免在BroadcastReceiver中使用依赖注入）
                val taskTitle = intent.getStringExtra("task_title") ?: "任务提醒"
                val taskLocation = intent.getStringExtra("task_location") ?: "目标位置"
                val taskLatitude = intent.getDoubleExtra("task_latitude", 0.0)
                val taskLongitude = intent.getDoubleExtra("task_longitude", 0.0)
                val geofenceRadius = intent.getFloatExtra("geofence_radius", 200f)
                
                if (taskLatitude == 0.0 || taskLongitude == 0.0) {
                    Timber.w("任务位置信息不完整，跳过位置提醒: taskId=$taskId")
                    return@launch
                }
                
                // 获取当前位置
                val currentLocation = getCurrentLocation(context)
                if (currentLocation == null) {
                    Timber.w("无法获取当前位置，跳过位置提醒: taskId=$taskId")
                    return@launch
                }
                
                // 计算距离
                val targetLocation = Location("task").apply {
                    latitude = taskLatitude
                    longitude = taskLongitude
                }
                
                val distance = currentLocation.distanceTo(targetLocation)
                
                Timber.d("位置验证: taskId=$taskId, 当前距离=${distance}m, 围栏半径=${geofenceRadius}m")
                
                // 只有当实际距离在围栏半径内时才发送通知
                if (distance <= geofenceRadius) {
                    CoroutineScope(Dispatchers.Main).launch {
                        notificationService.showLocationReminderNotification(
                            taskId = taskId,
                            taskTitle = taskTitle,
                            location = taskLocation
                        )
                    }
                    Timber.d("✅ 位置验证通过，发送通知: taskId=$taskId, 距离=${distance}m")
                } else {
                    Timber.w("❌ 位置验证失败，距离过远不发送通知: taskId=$taskId, 距离=${distance}m, 半径=${geofenceRadius}m")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "验证位置时发生异常: taskId=$taskId")
            }
        }
    }
    
    /**
     * 验证实际位置距离并发送通知（通过taskId获取任务信息）
     */
    private fun validateLocationAndNotifyWithTaskId(context: Context, geofenceId: String, taskId: Long, notificationService: NotificationService) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 由于在BroadcastReceiver中无法直接使用依赖注入，我们使用默认值
                // 在实际应用中，这里应该从数据库获取任务信息
                val taskTitle = "任务提醒"
                val taskLocation = "目标位置"
                val taskLatitude = 0.0
                val taskLongitude = 0.0
                val geofenceRadius = 200f
                
                if (taskLatitude == 0.0 || taskLongitude == 0.0) {
                    Timber.w("无法获取任务位置信息，跳过位置提醒: taskId=$taskId")
                    return@launch
                }
                
                // 获取当前位置
                val currentLocation = getCurrentLocation(context)
                if (currentLocation == null) {
                    Timber.w("无法获取当前位置，跳过位置提醒: taskId=$taskId")
                    return@launch
                }
                
                // 计算距离
                val targetLocation = Location("task").apply {
                    latitude = taskLatitude
                    longitude = taskLongitude
                }
                
                val distance = currentLocation.distanceTo(targetLocation)
                
                Timber.d("位置验证: taskId=$taskId, 当前距离=${distance}m, 围栏半径=${geofenceRadius}m")
                
                // 只有当实际距离在围栏半径内时才发送通知
                if (distance <= geofenceRadius) {
                    CoroutineScope(Dispatchers.Main).launch {
                        notificationService.showLocationReminderNotification(
                            taskId = taskId,
                            taskTitle = taskTitle,
                            location = taskLocation
                        )
                    }
                    Timber.d("✅ 位置验证通过，发送通知: taskId=$taskId, 距离=${distance}m")
                } else {
                    Timber.w("❌ 位置验证失败，距离过远不发送通知: taskId=$taskId, 距离=${distance}m, 半径=${geofenceRadius}m")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "验证位置时发生异常: taskId=$taskId")
            }
        }
    }
    
    /**
     * 获取当前位置
     */
    private fun getCurrentLocation(context: Context): Location? {
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
            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "获取当前位置时发生异常")
            null
        }
    }
}
