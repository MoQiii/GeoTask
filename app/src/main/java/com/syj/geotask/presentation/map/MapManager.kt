package com.syj.geotask.presentation.map

import android.content.Context
import android.os.Bundle
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
        // 创建高德地图提供者实例
        aMapProvider = AMapProvider()
        currentProvider = aMapProvider
        
        // 初始化高德地图提供者
        currentProvider?.initialize(context, MapConfig.getApiKey(context, "AMap"))
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
     */
    fun onResume() {
        aMapProvider?.onResume()
    }
    
    fun onPause() {
        aMapProvider?.onPause()
    }
    
    fun onDestroy() {
        aMapProvider?.onDestroy()
        aMapProvider = null
        currentProvider = null
    }
    
    fun onLowMemory() {
        aMapProvider?.onLowMemory()
    }
    
    fun onSaveInstanceState(outState: Bundle) {
        aMapProvider?.onSaveInstanceState(outState)
    }
}
