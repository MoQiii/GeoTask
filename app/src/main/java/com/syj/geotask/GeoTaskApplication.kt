package com.syj.geotask

import android.app.Application
import com.syj.geotask.presentation.map.MapManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeoTaskApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
