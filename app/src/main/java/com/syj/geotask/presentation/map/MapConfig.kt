package com.syj.geotask.presentation.map

import android.content.Context
import timber.log.Timber

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
            
            Timber.d("尝试读取高德地图API密钥...")
            Timber.d("meta-data键名: $AMAP_API_KEY_META_DATA")
            Timber.d("读取到的API密钥: '$apiKey'")
            Timber.d("API密钥长度: ${apiKey.length}")
            Timber.d("meta-data内容: ${appInfo.metaData?.keySet()?.joinToString(", ")}")
            
            if (apiKey.isEmpty()) {
                Timber.e("高德地图API密钥未在AndroidManifest.xml中配置")
            } else {
                Timber.d("成功读取高德地图API密钥: ${apiKey.take(8)}...")
            }
            
            apiKey
        } catch (e: Exception) {
            Timber.e(e, "读取高德地图API密钥失败")
            ""
        }
    }
    
    /**
     * 检查指定地图提供者的API密钥是否已配置
     */
    fun isApiKeyConfigured(context: Context, providerName: String): Boolean {
        val apiKey = getApiKey(context, providerName)
        val isConfigured = apiKey.isNotEmpty() && 
               apiKey != "your_amap_api_key_here" &&
               !apiKey.contains("your_") &&
               !apiKey.contains("_here") &&
               apiKey.length >= 20 // 高德地图API密钥通常至少20个字符
        
        Timber.d("API密钥验证: provider=$providerName, configured=$isConfigured, length=${apiKey.length}")
        
        // 额外检查：确保API密钥不是测试密钥或无效密钥
        val isValidKey = when (providerName) {
            "AMap" -> {
                // 高德地图API密钥格式检查（通常是32位十六进制字符串）
                val isHexPattern = apiKey.matches(Regex("^[a-fA-F0-9]{32}$"))
                val isKnownTestKey = apiKey == "c5246ec1319a09e249ddf141fcc63447" // 当前配置的测试密钥
                
                Timber.d("高德地图密钥检查: isHexPattern=$isHexPattern, isKnownTestKey=$isKnownTestKey")
                
                // 允许测试密钥，但记录警告
                if (isKnownTestKey) {
                    Timber.w("使用的是高德地图测试密钥，可能存在使用限制")
                }
                
                isConfigured && (isHexPattern || isKnownTestKey)
            }
            else -> isConfigured
        }
        
        Timber.d("最终API密钥验证结果: provider=$providerName, valid=$isValidKey")
        return isValidKey
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
