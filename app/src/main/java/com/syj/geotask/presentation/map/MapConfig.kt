package com.syj.geotask.presentation.map

import android.content.Context
import android.util.Log

/**
 * 地图配置类
 * 集中管理所有地图相关的配置信息
 * 从AndroidManifest.xml中读取API密钥
 */
object MapConfig {
    
    // 默认地图提供者
    const val DEFAULT_MAP_PROVIDER = "AMap"
    
    // 高德地图API密钥的meta-data键名
    private const val AMAP_API_KEY_META_DATA = "com.amap.api.v2.apikey"
    
    /**
     * 从AndroidManifest.xml中获取指定地图提供者的API密钥
     */
    fun getApiKey(context: Context, providerName: String): String {
        return when (providerName) {
            "AMap" -> getAmapApiKey(context)
            else -> ""
        }
    }
    
    /**
     * 从AndroidManifest.xml中获取高德地图API密钥
     */
    private fun getAmapApiKey(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, 
                android.content.pm.PackageManager.GET_META_DATA
            )
            val apiKey = appInfo.metaData?.getString(AMAP_API_KEY_META_DATA) ?: ""
            
            Log.d("MapConfig", "尝试读取高德地图API密钥...")
            Log.d("MapConfig", "meta-data键名: $AMAP_API_KEY_META_DATA")
            Log.d("MapConfig", "读取到的API密钥: '$apiKey'")
            Log.d("MapConfig", "API密钥长度: ${apiKey.length}")
            Log.d("MapConfig", "meta-data内容: ${appInfo.metaData?.keySet()?.joinToString(", ")}")
            
            if (apiKey.isEmpty()) {
                Log.e("MapConfig", "高德地图API密钥未在AndroidManifest.xml中配置")
            } else {
                Log.d("MapConfig", "成功读取高德地图API密钥: ${apiKey.take(8)}...")
            }
            
            apiKey
        } catch (e: Exception) {
            Log.e("MapConfig", "读取高德地图API密钥失败", e)
            ""
        }
    }
    
    /**
     * 检查指定地图提供者的API密钥是否已配置
     */
    fun isApiKeyConfigured(context: Context, providerName: String): Boolean {
        val apiKey = getApiKey(context, providerName)
        return apiKey.isNotEmpty() && 
               apiKey != "your_amap_api_key_here" &&
               !apiKey.contains("your_") &&
               !apiKey.contains("_here") &&
               apiKey.length >= 20 // 高德地图API密钥通常至少20个字符
    }
    
    /**
     * 获取所有支持的地图提供者
     */
    fun getSupportedProviders(): List<String> {
        return listOf("AMap")
    }
    
    /**
     * 检查地图提供者是否受支持
     */
    fun isProviderSupported(providerName: String): Boolean {
        return getSupportedProviders().contains(providerName)
    }
}
