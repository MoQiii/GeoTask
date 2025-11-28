package com.syj.geotask.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 地图提供者抽象接口
 * 用于支持不同地图实现的切换
 */
interface MapProvider {
    
    /**
     * 地图视图组件
     * @param modifier 修饰符
     * @param initialLat 初始纬度
     * @param initialLng 初始经度
     * @param onLocationSelected 位置选择回调
     * @param apiKey 地图API密钥
     */
    @Composable
    fun MapView(
        modifier: Modifier,
        initialLat: Double,
        initialLng: Double,
        onLocationSelected: (String, Double, Double) -> Unit,
        apiKey: String
    )
    
    /**
     * 获取地图提供者名称
     */
    fun getProviderName(): String
    
    /**
     * 初始化地图
     * @param context 上下文
     * @param apiKey API密钥
     */
    fun initialize(context: android.content.Context, apiKey: String)
    
    /**
     * 检查地图是否已初始化
     */
    fun isInitialized(): Boolean
}
