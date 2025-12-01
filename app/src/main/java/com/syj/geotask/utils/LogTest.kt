package com.syj.geotask.utils

import android.content.Context
import timber.log.Timber

/**
 * 日志测试工具类
 */
object LogTest {
    
    /**
     * 测试Timber日志功能
     */
    fun testLogging(context: Context) {
        Timber.d("=== 开始测试Timber日志功能 ===")
        
        // 测试不同级别的日志
        Timber.v("这是一条VERBOSE级别的日志")
        Timber.d("这是一条DEBUG级别的日志")
        Timber.i("这是一条INFO级别的日志")
        Timber.w("这是一条WARN级别的日志")
        Timber.e("这是一条ERROR级别的日志")
        
        // 测试带异常的日志
        try {
            throw RuntimeException("这是一个测试异常")
        } catch (e: Exception) {
            Timber.e(e, "捕获到测试异常")
        }
        
        // 测试带标签的日志
        Timber.tag("MapTest").d("地图功能测试日志")
        Timber.tag("NetworkTest").i("网络功能测试日志")
        
        // 测试文件日志功能
        val logFiles = getFileLoggingTree(context)?.getLogFiles()
        if (logFiles != null && logFiles.isNotEmpty()) {
            Timber.i("日志文件数量: ${logFiles.size}")
            logFiles.forEach { file ->
                Timber.i("日志文件: ${file.name}, 大小: ${file.length()} bytes")
            }
        } else {
            Timber.w("未找到日志文件")
        }
        
        Timber.d("=== Timber日志功能测试完成 ===")
    }
    
    /**
     * 获取FileLoggingTree实例
     */
    private fun getFileLoggingTree(context: Context): FileLoggingTree? {
        // 这里需要从Timber中获取我们的FileLoggingTree实例
        // 由于Timber不提供获取已注册树的方法，我们返回null
        // 在实际使用中，可以通过Application类来获取
        return null
    }
    
    /**
     * 清理所有日志文件
     */
    fun clearAllLogs(context: Context) {
        try {
            val logDirectory = java.io.File(context.filesDir, "log")
            if (logDirectory.exists()) {
                logDirectory.listFiles { file ->
                    file.isFile && file.name.startsWith("geotask_") && file.name.endsWith(".log")
                }?.forEach { file ->
                    if (file.delete()) {
                        Timber.d("删除日志文件: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理日志文件失败")
        }
    }
}
