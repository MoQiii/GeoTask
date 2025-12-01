package com.syj.geotask.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationService = NotificationService(context)
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null || geofencingEvent.hasError()) {
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
                    // 发送通知（暂时不获取任务信息，避免依赖注入问题）
                    CoroutineScope(Dispatchers.Main).launch {
                        notificationService.showLocationReminderNotification(
                            taskId = taskId,
                            taskTitle = "位置提醒",
                            location = "您已到达目标位置"
                        )
                    }
                }
            }
        }
    }
}
