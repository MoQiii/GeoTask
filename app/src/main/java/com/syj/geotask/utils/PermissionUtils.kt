package com.syj.geotask.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 权限工具类
 * 用于检查和请求应用所需的各种权限
 */
object PermissionUtils {
    
    /**
     * 检查是否有位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查是否有后台位置权限
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return hasLocationPermission(context)
    }
    
    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Android 13以下不需要通知权限
    }
    
    /**
     * 检查是否有网络权限
     */
    fun hasNetworkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true // Android 13+ 不需要存储权限
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查应用所需的所有关键权限
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasLocationPermission(context) &&
               hasNetworkPermission(context) &&
               hasNotificationPermission(context)
    }
    
    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasLocationPermission(context)) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (!hasNetworkPermission(context)) {
            missingPermissions.add(Manifest.permission.INTERNET)
            missingPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !hasStoragePermission(context)) {
            missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        return missingPermissions
    }
    
    /**
     * 检查权限是否会影响地图功能
     */
    fun canUseMapFeatures(context: Context): Boolean {
        return hasNetworkPermission(context) && hasLocationPermission(context)
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatus(context: Context): Map<String, Boolean> {
        return mapOf(
            "位置权限" to hasLocationPermission(context),
            "后台位置权限" to hasBackgroundLocationPermission(context),
            "通知权限" to hasNotificationPermission(context),
            "网络权限" to hasNetworkPermission(context),
            "存储权限" to hasStoragePermission(context)
        )
    }
    
    /**
     * 获取地图功能所需的权限列表
     */
    fun getMapPermissions(): List<String> {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
    }
    
    /**
     * 获取所有应用所需权限列表
     */
    fun getAllRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(getMapPermissions())
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        return permissions
    }
}

/**
 * 权限请求Compose组件
 * 用于在Compose中请求权限
 */
@Composable
fun RequestPermissions(
    permissions: List<String>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit
) {
    val context = LocalContext.current
    
    // 权限请求launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        onPermissionsResult(permissionsMap)
    }
    
    // 检查是否需要请求权限
    LaunchedEffect(permissions) {
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // 如果所有权限都已授予，直接返回成功状态
            onPermissionsResult(permissions.associateWith { true })
        }
    }
}

/**
 * 请求地图功能所需权限的便捷函数
 */
@Composable
fun RequestMapPermissions(
    onPermissionsResult: (Boolean) -> Unit
) {
    RequestPermissions(
        permissions = PermissionUtils.getMapPermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        onPermissionsResult(allGranted)
    }
}

/**
 * 请求所有应用所需权限的便捷函数
 */
@Composable
fun RequestAllRequiredPermissions(
    onPermissionsResult: (Boolean) -> Unit
) {
    RequestPermissions(
        permissions = PermissionUtils.getAllRequiredPermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        onPermissionsResult(allGranted)
    }
}
