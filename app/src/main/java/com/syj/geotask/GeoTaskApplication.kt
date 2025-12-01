package com.syj.geotask

import android.app.Application
import com.syj.geotask.presentation.map.MapManager
import com.syj.geotask.utils.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GeoTaskApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化Timber日志框架
        initializeTimber()
    }
    
    private fun initializeTimber() {
        // 同时输出到控制台和文件
        Timber.plant(Timber.DebugTree())
        
        // 文件输出
        val fileLoggingTree = FileLoggingTree(this)
        Timber.plant(fileLoggingTree)
        
        Timber.d("Timber初始化完成 - 控制台+文件日志")
    }
}
