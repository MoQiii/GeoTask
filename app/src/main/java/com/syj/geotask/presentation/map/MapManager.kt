package com.syj.geotask.presentation.map

import android.content.Context
import android.os.Bundle
import timber.log.Timber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 地图管理器
 * 用于管理高德地图提供者，提供统一的接口
 */
object MapManager {
    
    private var currentProvider: MapProvider? = null
    private var aMapProvider: AMapProvider? = null
    
    /**
     * 初始化地图管理器
     */
    fun initialize(context: Context) {
        try {
            Timber.d("开始初始化地图管理器")
            
            // 检查API密钥配置
            val apiKeyConfigured = MapConfig.isApiKeyConfigured(context, "AMap")
            Timber.d("API密钥配置状态: $apiKeyConfigured")
            
            if (!apiKeyConfigured) {
                Timber.e("高德地图API密钥未配置或无效，跳过初始化")
                return
            }
            
            // 创建高德地图提供者实例
            aMapProvider = AMapProvider()
            currentProvider = aMapProvider
            
            // 获取API密钥并初始化高德地图提供者
            val apiKey = MapConfig.getApiKey(context, "AMap")
            Timber.d("使用API密钥初始化: ${apiKey.take(8)}...")
            
            currentProvider?.initialize(context, apiKey)
            
            // 验证初始化结果
            val isInitialized = currentProvider?.isInitialized() ?: false
            Timber.d("地图提供者初始化结果: $isInitialized")
            
            if (!isInitialized) {
                Timber.e("地图提供者初始化失败")
                currentProvider = null
                aMapProvider = null
            } else {
                Timber.d("地图管理器初始化成功")
            }
        } catch (e: Exception) {
            Timber.e(e, "地图管理器初始化异常")
            currentProvider = null
            aMapProvider = null
        }
    }
    
    /**
     * 获取当前地图提供者
     */
    fun getCurrentProvider(): MapProvider? = currentProvider
    
    /**
     * 获取当前提供者名称
     */
    fun getCurrentProviderName(): String = currentProvider?.getProviderName() ?: "None"
    
    /**
     * 检查当前提供者是否已初始化
     */
    fun isCurrentProviderInitialized(): Boolean = currentProvider?.isInitialized() ?: false
    
    /**
     * 检查高德地图API密钥是否已配置
     */
    fun isProviderConfigured(context: Context): Boolean {
        return MapConfig.isApiKeyConfigured(context, "AMap")
    }
    
    /**
     * 地图视图组件
     */
    @Composable
    fun MapView(
        modifier: Modifier,
        initialLat: Double,
        initialLng: Double,
        onLocationSelected: (String, Double, Double) -> Unit,
        context: Context
    ) {
        val apiKey = MapConfig.getApiKey(context, "AMap")
        
        currentProvider?.MapView(
            modifier = modifier,
            initialLat = initialLat,
            initialLng = initialLng,
            onLocationSelected = onLocationSelected,
            apiKey = apiKey
        )
    }
    
    /**
     * 处理地图生命周期事件
     * 注意：现在生命周期由AMapProvider内部通过DisposableEffect自动管理
     * 这些方法保留用于兼容性，但实际不再需要手动调用
     */
    @Deprecated("生命周期现在由AMapProvider内部自动管理", level = DeprecationLevel.WARNING)
    fun onResume() {
        // 不再需要手动调用，生命周期由DisposableEffect处理
    }
    
    @Deprecated("生命周期现在由AMapProvider内部自动管理", level = DeprecationLevel.WARNING)
    fun onPause() {
        // 不再需要手动调用，生命周期由DisposableEffect处理
    }
    
    @Deprecated("生命周期现在由AMapProvider内部自动管理", level = DeprecationLevel.WARNING)
    fun onDestroy() {
        // 不再需要手动调用，生命周期由DisposableEffect处理
    }
    
    /**
     * 完全销毁地图管理器（仅在应用退出时调用）
     */
    fun destroyCompletely() {
        aMapProvider = null
        currentProvider = null
    }
    
    @Deprecated("生命周期现在由AMapProvider内部自动管理", level = DeprecationLevel.WARNING)
    fun onLowMemory() {
        // 不再需要手动调用，生命周期由DisposableEffect处理
    }
    
    @Deprecated("生命周期现在由AMapProvider内部自动管理", level = DeprecationLevel.WARNING)
    fun onSaveInstanceState(outState: Bundle) {
        // 不再需要手动调用，生命周期由DisposableEffect处理
    }
}
