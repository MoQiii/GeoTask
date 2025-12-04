package com.syj.geotask.data.service

import android.content.Context
import android.location.Location
import android.os.Looper
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.syj.geotask.utils.PermissionUtils
import timber.log.Timber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 高德地图定位服务
 * 提供高精度的位置获取功能，替代 Android 原生 LocationManager
 */
class AMapLocationService(private val context: Context) {

    private var locationClient: AMapLocationClient? = null
    private var locationOption: AMapLocationClientOption? = null

    companion object {
        private const val LOCATION_TIMEOUT = 10000L // 10秒超时
    }

    init {
        initializeLocationClient()
    }

    /**
     * 初始化高德定位客户端
     */
    private fun initializeLocationClient() {
        try {
            locationClient = AMapLocationClient(context).apply {
                // 设置定位参数
                locationOption = AMapLocationClientOption().apply {
                    // 设置定位模式为高精度模式
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    
                    // 设置定位间隔，这里设置为单次定位，所以不设置间隔
                    // interval = 2000L
                    
                    // 设置是否返回地址信息（默认返回）
                    isNeedAddress = true
                    
                    // 设置是否只定位一次，默认为false
                    isOnceLocation = true
                    
                    // 设置是否强制刷新WIFI，默认为强制刷新
                    isWifiActiveScan = true
                    
                    // 设置是否允许模拟位置，默认为false，不允许模拟位置
                    isMockEnable = false
                    
                    // 设置定位超时时间，单位是毫秒，默认值是30000毫秒
                    httpTimeOut = LOCATION_TIMEOUT
                }
                
                // 设置定位参数
                setLocationOption(locationOption)
            }
            
            Timber.d("✅ 高德定位客户端初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ 高德定位客户端初始化失败")
            throw e
        }
    }

    /**
     * 获取当前位置（高精度）
     * @return Location对象，如果获取失败返回null
     */
    suspend fun getCurrentLocation(): Location? {
        return try {
            // 检查位置权限
            if (!PermissionUtils.hasLocationPermission(context)) {
                Timber.w("❌ 位置权限未授予，无法获取位置")
                return null
            }

            // 检查定位客户端是否可用
            if (locationClient == null) {
                Timber.e("❌ 定位客户端未初始化")
                return null
            }

            Timber.d("📍 开始高德定位，定位模式: ${locationOption?.locationMode}")
            Timber.d("📍 定位参数 - 单次定位: ${locationOption?.isOnceLocation}, 需要地址: ${locationOption?.isNeedAddress}")

            // 使用协程等待定位结果
            suspendCancellableCoroutine { continuation ->
                val locationListener = object : AMapLocationListener {
                    override fun onLocationChanged(amapLocation: AMapLocation?) {
                        try {
                            Timber.d("📍 高德定位回调触发")
                            
                            if (amapLocation != null) {
                                Timber.d("📍 定位结果 - errorCode: ${amapLocation.errorCode}, errorInfo: ${amapLocation.errorInfo}")
                                Timber.d("📍 定位详情 - lat: ${amapLocation.latitude}, lng: ${amapLocation.longitude}, accuracy: ${amapLocation.accuracy}")
                                Timber.d("📍 定位类型: ${amapLocation.locationType}, 提供者: ${amapLocation.provider}")
                                
                                if (amapLocation.errorCode == 0) {
                                    // 定位成功
                                    val location = convertToAndroidLocation(amapLocation)
                                    Timber.d("✅ 高德定位成功: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
                                    Timber.d("✅ 定位地址: ${amapLocation.address}")
                                    continuation.resume(location)
                                } else {
                                    // 定位失败
                                    val errorMsg = "高德定位失败: errorCode=${amapLocation.errorCode}, errorInfo=${amapLocation.errorInfo}"
                                    Timber.e(errorMsg)
                                    
                                    // 根据错误码提供更详细的信息
                                    when (amapLocation.errorCode) {
                                        1 -> Timber.e("❌ 定位失败原因: 权限不足")
                                        2 -> Timber.e("❌ 定位失败原因: 网络异常")
                                        3 -> Timber.e("❌ 定位失败原因: 定位服务未开启")
                                        4 -> Timber.e("❌ 定位失败原因: 定位模式错误")
                                        5 -> Timber.e("❌ 定位失败原因: 设备不支持定位")
                                        else -> Timber.e("❌ 定位失败原因: 未知错误")
                                    }
                                    
                                    continuation.resume(null)
                                }
                            } else {
                                Timber.e("❌ 高德定位返回null")
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "❌ 处理高德定位结果时发生异常")
                            continuation.resumeWithException(e)
                        } finally {
                            // 停止定位
                            try {
                                locationClient?.stopLocation()
                                Timber.d("📍 定位已停止")
                            } catch (e: Exception) {
                                Timber.e(e, "❌ 停止定位时发生异常")
                            }
                        }
                    }
                }

                try {
                    // 设置定位监听器
                    locationClient?.setLocationListener(locationListener)
                    
                    // 启动定位
                    Timber.d("📍 启动高德定位...")
                    val startResult = locationClient?.startLocation()
                    Timber.d("📍 启动定位结果: $startResult")
                    
                    // 设置超时处理
                    continuation.invokeOnCancellation {
                        try {
                            Timber.d("📍 定位被取消，停止定位服务")
                            locationClient?.stopLocation()
                            locationClient?.unRegisterLocationListener(locationListener)
                        } catch (e: Exception) {
                            Timber.e(e, "❌ 取消定位时发生异常")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ 启动定位时发生异常")
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 获取高德定位时发生异常")
            null
        }
    }

    /**
     * 将高德定位结果转换为 Android Location 对象
     */
    private fun convertToAndroidLocation(amapLocation: AMapLocation): Location {
        return Location("amap").apply {
            latitude = amapLocation.latitude
            longitude = amapLocation.longitude
            accuracy = amapLocation.accuracy.toFloat()
            
            // 设置时间戳
            time = amapLocation.time
            
            // 设置速度（如果有）
            if (amapLocation.speed != 0f) {
                speed = amapLocation.speed
            }
            
            // 设置方向（如果有）
            if (amapLocation.bearing != 0f) {
                bearing = amapLocation.bearing
            }
            
            // 设置海拔（如果有）
            if (amapLocation.altitude != 0.0) {
                altitude = amapLocation.altitude
            }
            
            // 设置提供者
            provider = when (amapLocation.locationType) {
                AMapLocation.LOCATION_TYPE_GPS -> "GPS"
                AMapLocation.LOCATION_TYPE_SAME_REQ -> "Network"
                AMapLocation.LOCATION_TYPE_WIFI -> "WiFi"
                AMapLocation.LOCATION_TYPE_CELL -> "Cell"
                else -> "AMap"
            }
        }
    }

    /**
     * 获取当前位置（包含地址信息）
     * @return Pair<Location?, String?>，第一个是位置，第二个是地址
     */
    suspend fun getCurrentLocationWithAddress(): Pair<Location?, String?> {
        return try {
            // 检查位置权限
            if (!PermissionUtils.hasLocationPermission(context)) {
                Timber.w("❌ 位置权限未授予，无法获取位置")
                return Pair(null, null)
            }

            // 检查定位客户端是否可用
            if (locationClient == null) {
                Timber.e("❌ 定位客户端未初始化")
                return Pair(null, null)
            }

            Timber.d("📍 开始高德定位（包含地址信息）")

            // 使用协程等待定位结果
            suspendCancellableCoroutine { continuation ->
                val locationListener = object : AMapLocationListener {
                    override fun onLocationChanged(amapLocation: AMapLocation?) {
                        try {
                            Timber.d("📍 高德定位回调触发")
                            
                            if (amapLocation != null) {
                                Timber.d("📍 定位结果 - errorCode: ${amapLocation.errorCode}, errorInfo: ${amapLocation.errorInfo}")
                                Timber.d("📍 定位详情 - lat: ${amapLocation.latitude}, lng: ${amapLocation.longitude}, accuracy: ${amapLocation.accuracy}")
                                Timber.d("📍 定位类型: ${amapLocation.locationType}, 提供者: ${amapLocation.provider}")
                                
                                if (amapLocation.errorCode == 0) {
                                    // 定位成功
                                    val location = convertToAndroidLocation(amapLocation)
                                    val address = amapLocation.address
                                    
                                    Timber.d("✅ 高德定位成功: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
                                    Timber.d("✅ 定位地址: $address")
                                    
                                    continuation.resume(Pair(location, address))
                                } else {
                                    // 定位失败
                                    val errorMsg = "高德定位失败: errorCode=${amapLocation.errorCode}, errorInfo=${amapLocation.errorInfo}"
                                    Timber.e(errorMsg)
                                    
                                    // 根据错误码提供更详细的信息
                                    when (amapLocation.errorCode) {
                                        1 -> Timber.e("❌ 定位失败原因: 权限不足")
                                        2 -> Timber.e("❌ 定位失败原因: 网络异常")
                                        3 -> Timber.e("❌ 定位失败原因: 定位服务未开启")
                                        4 -> Timber.e("❌ 定位失败原因: 定位模式错误")
                                        5 -> Timber.e("❌ 定位失败原因: 设备不支持定位")
                                        else -> Timber.e("❌ 定位失败原因: 未知错误")
                                    }
                                    
                                    continuation.resume(Pair(null, null))
                                }
                            } else {
                                Timber.e("❌ 高德定位返回null")
                                continuation.resume(Pair(null, null))
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "❌ 处理高德定位结果时发生异常")
                            continuation.resumeWithException(e)
                        } finally {
                            // 停止定位
                            try {
                                locationClient?.stopLocation()
                                Timber.d("📍 定位已停止")
                            } catch (e: Exception) {
                                Timber.e(e, "❌ 停止定位时发生异常")
                            }
                        }
                    }
                }

                try {
                    // 设置定位监听器
                    locationClient?.setLocationListener(locationListener)
                    
                    // 启动定位
                    Timber.d("📍 启动高德定位...")
                    val startResult = locationClient?.startLocation()
                    Timber.d("📍 启动定位结果: $startResult")
                    
                    // 设置超时处理
                    continuation.invokeOnCancellation {
                        try {
                            Timber.d("📍 定位被取消，停止定位服务")
                            locationClient?.stopLocation()
                            locationClient?.unRegisterLocationListener(locationListener)
                        } catch (e: Exception) {
                            Timber.e(e, "❌ 取消定位时发生异常")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ 启动定位时发生异常")
                    continuation.resume(Pair(null, null))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 获取高德定位时发生异常")
            Pair(null, null)
        }
    }

    /**
     * 获取最后一次已知位置
     * @return Location对象，如果没有缓存位置返回null
     */
    fun getLastKnownLocation(): Location? {
        return try {
            val amapLocation = locationClient?.lastKnownLocation
            if (amapLocation != null && amapLocation.errorCode == 0) {
                val location = convertToAndroidLocation(amapLocation)
                Timber.d("✅ 获取高德缓存位置成功: lat=${location.latitude}, lng=${location.longitude}")
                location
            } else {
                Timber.d("⚠️ 没有高德缓存位置")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 获取高德缓存位置时发生异常")
            null
        }
    }

    /**
     * 销毁定位服务，释放资源
     */
    fun destroy() {
        try {
            locationClient?.stopLocation()
            locationClient?.onDestroy()
            locationClient = null
            locationOption = null
            Timber.d("✅ 高德定位服务已销毁")
        } catch (e: Exception) {
            Timber.e(e, "❌ 销毁高德定位服务时发生异常")
        }
    }

    /**
     * 检查高德定位服务是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            locationClient != null && locationOption != null
        } catch (e: Exception) {
            Timber.e(e, "❌ 检查高德定位服务可用性时发生异常")
            false
        }
    }
}
