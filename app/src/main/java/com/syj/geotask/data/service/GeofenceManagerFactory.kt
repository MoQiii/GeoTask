package com.syj.geotask.data.service

import android.content.Context
import com.syj.geotask.domain.model.Task
import timber.log.Timber

/**
 * 地理围栏管理器工厂
 * 专门使用高德地图地理围栏服务
 * 解决国内环境下的兼容性问题，避免Google Play Services依赖
 */
class GeofenceManagerFactory(private val context: Context) {

    /**
     * 获取地理围栏管理器实例
     * 直接返回高德地图地理围栏管理器
     */
    fun getGeofenceManager(): IGeofenceManager {
        return try {
            val aMapManager = AMapGeofenceManager(context)
            Timber.d("✅ 使用高德地图地理围栏服务")
            aMapManager
        } catch (e: Exception) {
            Timber.e(e, "❌ 高德地图地理围栏服务初始化失败")
            throw RuntimeException("高德地图地理围栏服务不可用，请检查高德地图SDK配置", e)
        }
    }

    /**
     * 获取当前使用的地理围栏服务类型
     */
    fun getCurrentServiceType(): String {
        return "高德地图地理围栏"
    }

    /**
     * 获取高德地图地理围栏管理器（访问高德特有功能）
     */
    fun getAMapGeofenceManager(): AMapGeofenceManager {
        return try {
            AMapGeofenceManager(context)
        } catch (e: Exception) {
            Timber.e(e, "创建高德地图地理围栏管理器失败")
            throw RuntimeException("高德地图地理围栏服务不可用", e)
        }
    }
}

/**
 * 地理围栏管理器接口
 * 统一不同地理围栏服务的API
 */
interface IGeofenceManager {
    /**
     * 为任务添加地理围栏
     */
    suspend fun addGeofenceForTask(task: Task): Boolean

    /**
     * 移除指定任务的地理围栏
     */
    suspend fun removeGeofenceForTask(taskId: Long): Boolean

    /**
     * 移除所有地理围栏
     */
    suspend fun removeAllGeofences(): Boolean
}
