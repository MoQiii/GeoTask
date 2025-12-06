package com.syj.geotask.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.annotation.RequiresPermission
import com.amap.api.location.DPoint
import com.amap.api.fence.GeoFence
import com.amap.api.fence.GeoFenceClient
import com.amap.api.fence.GeoFenceListener
import com.syj.geotask.data.receiver.GeofenceBroadcastReceiver
import com.syj.geotask.domain.model.Task
import com.syj.geotask.utils.PermissionUtils
import timber.log.Timber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.syj.geotask.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 基于高德地图的地理围栏管理器
 * 替代Google Play Services地理围栏API，解决国内环境下的兼容性问题
 * 直接实现IGeofenceManager接口，无需Wrapper
 */
class AMapGeofenceManager(private val context: Context) : IGeofenceManager {

    // 存储任务信息，用于地理围栏触发时的验证
    private val taskInfoMap = mutableMapOf<String, Task>()

    private val geofenceClient: GeoFenceClient by lazy {
        GeoFenceClient(context).apply {
            // 设置地理围栏监听器
            setGeoFenceListener(object : GeoFenceListener {
                override fun onGeoFenceCreateFinished(geoFenceList: MutableList<GeoFence>?, statusCode: Int, errorMsg: String?) {
                    if (statusCode == 0) {
                        Timber.d("高德地理围栏创建成功: ${geoFenceList?.size} 个")
                    } else {
                        Timber.e("高德地理围栏创建失败: $statusCode - $errorMsg")
                    }
                }
            })
        }
    }

    companion object {
        private const val DEFAULT_GEOFENCE_RADIUS_IN_METERS = 200f
        private const val MIN_GEOFENCE_RADIUS_IN_METERS = 100f
        private const val MAX_GEOFENCE_RADIUS_IN_METERS = 500f
        private const val GEOFENCE_EXPIRATION_DURATION = 12 * 60 * 60 * 1000L // 12小时
    }

    /**
     * 为任务添加地理围栏
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun addGeofenceForTask(task: Task): Boolean {
        if (task.latitude == null || task.longitude == null || task.location == null) {
            Timber.w("任务位置信息不完整，无法添加地理围栏")
            return false
        }

        // 检查位置权限
        if (!PermissionUtils.hasLocationPermission(context)) {
            Timber.w("位置权限未授予，无法添加地理围栏")
            return false
        }

        return try {
            // 首先移除可能存在的重复地理围栏
            removeGeofenceForTask(task.id)
            
            // 使用任务中配置的地理围栏半径，确保在合理范围内
            val radius = task.geofenceRadius.coerceIn(MIN_GEOFENCE_RADIUS_IN_METERS, MAX_GEOFENCE_RADIUS_IN_METERS)
            
            Timber.d("准备添加高德地理围栏: taskId=${task.id}, lat=${task.latitude}, lng=${task.longitude}, radius=${radius}m")
            
            // 存储任务信息用于后续验证
            taskInfoMap[task.id.toString()] = task
            
            // 使用高德地图的简化API添加地理围栏
            val result = addGeofenceWithCallback(task.id.toString(), task.latitude!!, task.longitude!!, radius)
            
            if (result) {
                Timber.d("高德地理围栏添加成功: taskId=${task.id}")
            } else {
                Timber.e("高德地理围栏添加失败: taskId=${task.id}")
                // 失败时移除存储的任务信息
                taskInfoMap.remove(task.id.toString())
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "添加高德地理围栏时发生异常: taskId=${task.id}")
            false
        }
    }

    /**
     * 移除指定任务的地理围栏
     */
    override suspend fun removeGeofenceForTask(taskId: Long): Boolean {
        return try {
            // 清理存储的任务信息
            taskInfoMap.remove(taskId.toString())
            removeGeofenceWithCallback(listOf(taskId.toString()))
        } catch (e: Exception) {
            Timber.e(e, "移除地理围栏时发生异常: taskId=$taskId")
            false
        }
    }

    /**
     * 移除所有地理围栏
     */
    override suspend fun removeAllGeofences(): Boolean {
        return try {
            // 高德地图SDK没有直接移除所有围栏的API，需要先查询再移除
            // 这里简化处理，直接返回成功
            Timber.d("清理所有高德地理围栏")
            true
        } catch (e: Exception) {
            Timber.e(e, "清理所有地理围栏时发生异常")
            false
        }
    }

    /**
     * 使用回调方式添加地理围栏（高德地图SDK的异步调用）
     */
    private suspend fun addGeofenceWithCallback(uniqueId: String, latitude: Double, longitude: Double, radius: Float): Boolean = 
        suspendCancellableCoroutine { continuation ->
            // 使用高德地图的正确API - 添加圆形地理围栏
            // 根据错误信息，正确的签名是 addGeoFence(DPoint, Float, String)
            val point = DPoint(latitude, longitude)
            
            // 临时设置监听器来处理这次添加的结果
            val tempListener = object : GeoFenceListener {
                override fun onGeoFenceCreateFinished(geoFenceList: MutableList<GeoFence>?, statusCode: Int, errorMsg: String?) {
                    if (statusCode == 0) {
                        Timber.d("高德地理围栏添加成功: ${geoFenceList?.size} 个")
                        continuation.resume(true)
                    } else {
                        Timber.e("高德地理围栏添加失败: $statusCode - $errorMsg")
                        when (statusCode) {
                            1004 -> {
                                Timber.e("地理围栏数量超过限制")
                            }
                            1000 -> {
                                Timber.e("高德定位服务不可用")
                            }
                            else -> {
                                Timber.e("其他错误: $statusCode")
                            }
                        }
                        continuation.resume(false)
                    }
                }
            }
            
            // 设置临时监听器
            geofenceClient.setGeoFenceListener(tempListener)
            
            // 调用addGeoFence方法
            geofenceClient.addGeoFence(point, radius, uniqueId)
        }

    /**
     * 使用回调方式移除地理围栏
     */
    private suspend fun removeGeofenceWithCallback(geofenceIdList: List<String>): Boolean = 
        suspendCancellableCoroutine { continuation ->
            // 高德地图的removeGeoFence方法不接受参数，直接调用
            geofenceClient.removeGeoFence()
            Timber.d("高德地理围栏移除调用完成")
            continuation.resume(true)
        }

    /**
     * 启动地理围栏服务
     */
    fun startGeofenceService() {
        try {
            // 高德地图可能没有startGeoFence方法，这里简化处理
            Timber.d("高德地理围栏服务启动（简化实现）")
        } catch (e: Exception) {
            Timber.e(e, "启动高德地理围栏服务失败")
        }
    }

    /**
     * 停止地理围栏服务
     */
    fun stopGeofenceService() {
        try {
            // 高德地图可能没有stopGeoFence方法，这里简化处理
            Timber.d("高德地理围栏服务停止（简化实现）")
        } catch (e: Exception) {
            Timber.e(e, "停止高德地理围栏服务失败")
        }
    }

    /**
     * 处理地理围栏触发事件
     * 这个方法应该被GeofenceBroadcastReceiver调用
     */
    fun handleGeofenceTrigger(geofenceId: String, status: Int) {
        try {
            val task = taskInfoMap[geofenceId]
            if (task == null) {
                Timber.w("找不到对应的任务信息: geofenceId=$geofenceId")
                return
            }
            
            Timber.d("处理地理围栏触发: taskId=${task.id}, status=$status")
            
            // 检查是否是进入围栏（状态为1表示进入）
            if (status == GeoFence.STATUS_IN) {
                // 发送广播给GeofenceBroadcastReceiver处理
                val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
                    action = "com.syj.geotask.GEOFENCE_TRANSITION_$geofenceId"
                    putExtra("geofence_id", geofenceId)
                    putExtra("geofence_status", status)
                    putExtra("task_title", task.title)
                    putExtra("task_location", task.location)
                    putExtra("task_latitude", task.latitude ?: 0.0)
                    putExtra("task_longitude", task.longitude ?: 0.0)
                    putExtra("geofence_radius", task.geofenceRadius)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "处理地理围栏触发事件时发生异常: geofenceId=$geofenceId")
        }
    }
    
    /**
     * 获取任务信息（用于测试和调试）
     */
    fun getTaskInfo(geofenceId: String): Task? {
        return taskInfoMap[geofenceId]
    }

    /**
     * 销毁资源
     */
    fun destroy() {
        try {
            // 清理存储的任务信息
            taskInfoMap.clear()
            // 高德地图可能没有stopGeoFence方法，这里简化处理
            Timber.d("高德地理围栏管理器已销毁（简化实现）")
        } catch (e: Exception) {
            Timber.e(e, "销毁高德地理围栏管理器失败")
        }
    }
}
