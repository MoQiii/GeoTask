package com.syj.geotask.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amap.api.fence.GeoFence
import com.syj.geotask.data.service.AMapGeofenceManager
import timber.log.Timber

/**
 * 高德地图地理围栏广播接收器
 * 专门处理高德地图SDK发送的地理围栏触发广播
 */
//class AMapGeofenceReceiver : BroadcastReceiver() {
//
//    override fun onReceive(context: Context, intent: Intent) {
//        try {
//            val action = intent.action
//            Timber.d("高德地图地理围栏广播接收器收到广播: $action")
//
//            // 高德地图地理围栏触发的Action
//            if (action == "com.amap.geofence.action") {
//                val geofenceStatus = intent.getIntExtra("geofence_status", -1)
//                val geofenceId = intent.getStringExtra("geofence_id")
//                val customId = intent.getStringExtra("custom_id")
//
//                Timber.d("高德地理围栏触发: id=$geofenceId, customId=$customId, status=$geofenceStatus")
//
//                // 如果有customId，使用它作为geofenceId
//                val effectiveGeofenceId = customId ?: geofenceId
//
//                if (effectiveGeofenceId != null && geofenceStatus == GeoFence.STATUS_IN) {
//                    // 获取AMapGeofenceManager实例并处理触发事件
//                    // 注意：这里需要通过依赖注入或其他方式获取AMapGeofenceManager实例
//                    // 由于这是BroadcastReceiver，我们通过发送另一个广播来处理
//                    val forwardIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
//                        setAction("com.syj.geotask.GEOFENCE_TRANSITION_$effectiveGeofenceId")
//                        putExtra("geofence_id", effectiveGeofenceId)
//                        putExtra("geofence_status", geofenceStatus)
//                        putExtra("source", "amap")
//                    }
//                    context.sendBroadcast(forwardIntent)
//
//                    Timber.d("转发高德地理围栏触发事件到GeofenceBroadcastReceiver: $effectiveGeofenceId")
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "处理高德地图地理围栏广播时发生异常")
//        }
//    }
//}
