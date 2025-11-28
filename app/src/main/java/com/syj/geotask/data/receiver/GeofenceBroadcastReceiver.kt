package com.syj.geotask.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.syj.geotask.data.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationService = NotificationService(context)
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent?.hasError() == true) {
            return
        }

        // 获取触发的地理围栏转换
        val geofenceTransition = geofencingEvent?.geofenceTransition
        
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // 获取触发的地理围栏列表
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            
            triggeringGeofences?.forEach { geofence ->
                val taskId = geofence.requestId.toLongOrNull()
                
                if (taskId != null) {
                    // 在协程中发送通知
                    val service = notificationService
                    CoroutineScope(Dispatchers.Main).launch {
                        // 这里应该从数据库获取任务信息
                        // 为了简化，我们使用一个通用的通知
                        service.showLocationReminderNotification(
                            taskId = taskId,
                            taskTitle = "任务提醒",
                            location = "目标位置"
                        )
                    }
                }
            }
        }
    }
}
