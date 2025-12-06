package com.syj.geotask.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.syj.geotask.presentation.map.MapManager
import com.syj.geotask.presentation.theme.GeoTaskTheme
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 获取当前位置（使用高德地图定位服务）
 */
@SuppressLint("MissingPermission")
@Composable
private fun getCurrentLocation(
    onLocationReceived: (Double, Double) -> Unit,
    onLocationError: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(key1 = context) {
        try {
            // 检查权限
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("位置权限未授予")
                onLocationError()
                return@LaunchedEffect
            }
            
            // 使用高德地图定位服务
            val aMapLocationService = com.syj.geotask.data.service.AMapLocationService(context)

            try {
                // 高德地图定位是异步的，需要在协程中调用
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val currentLocation = aMapLocationService.getCurrentLocation()
                        if (currentLocation != null) {
                            Timber.d("高德地图实时定位成功: lat=${currentLocation.latitude}, lng=${currentLocation.longitude}")
                            onLocationReceived(currentLocation.latitude, currentLocation.longitude)
                        } else {
                            Timber.w("高德地图定位失败，尝试使用原生定位")
                            // 回退到原生定位
                            getNativeLocationSync(context, onLocationReceived, onLocationError)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "高德地图定位异常，回退到原生定位")
                        getNativeLocationSync(context, onLocationReceived, onLocationError)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "高德地图定位异常，回退到原生定位")
                getNativeLocationSync(context, onLocationReceived, onLocationError)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "获取当前位置异常")
            onLocationError()
        }
    }
}

/**
 * 原生定位方法（作为高德定位的备选方案）
 */
@SuppressLint("MissingPermission")
private suspend fun getNativeLocation(
    context: android.content.Context,
    onLocationReceived: (Double, Double) -> Unit,
    onLocationError: () -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // 获取最后已知位置
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Timber.d("原生定位成功: lat=${location.latitude}, lng=${location.longitude}")
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    Timber.w("原生定位也无法获取位置")
                    onLocationError()
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "原生定位失败")
                onLocationError()
            }
    } catch (e: Exception) {
        Timber.e(e, "原生定位异常")
        onLocationError()
    }
}

/**
 * 原生定位方法（同步版本，用于非协程环境）
 */
@SuppressLint("MissingPermission")
private fun getNativeLocationSync(
    context: android.content.Context,
    onLocationReceived: (Double, Double) -> Unit,
    onLocationError: () -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // 获取最后已知位置
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Timber.d("原生定位成功: lat=${location.latitude}, lng=${location.longitude}")
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    Timber.w("原生定位也无法获取位置")
                    onLocationError()
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "原生定位失败")
                onLocationError()
            }
    } catch (e: Exception) {
        Timber.e(e, "原生定位异常")
        onLocationError()
    }
}

/**
 * 请求位置更新
 */
@SuppressLint("MissingPermission")
private fun requestLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Double, Double) -> Unit,
    onLocationError: () -> Unit
) {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000L // 10秒
    ).apply {
        setMaxUpdateDelayMillis(30000L) // 最大30秒
        setMinUpdateIntervalMillis(5000L) // 最小5秒
    }.build()
    
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                Timber.d("位置更新成功: lat=${location.latitude}, lng=${location.longitude}")
                onLocationReceived(location.latitude, location.longitude)
                fusedLocationClient.removeLocationUpdates(this)
            } ?: run {
                Timber.w("位置更新结果为空")
                onLocationError()
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) {
                Timber.w("位置服务不可用")
                onLocationError()
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }
    
    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    } catch (e: Exception) {
        Timber.e(e, "请求位置更新异常")
        onLocationError()
    }
}

/**
 * 地图选择界面
 * 用于选择任务位置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onLocationSelected: (String, Double, Double) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedLocation by remember { mutableStateOf("") }
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLng by remember { mutableStateOf(0.0) }
    var selectedLat by remember { mutableStateOf(0.0) }
    var selectedLng by remember { mutableStateOf(0.0) }
    var isLocationConfirmed by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(true) }
    var hasValidLocation by remember { mutableStateOf(false) }
    
    // 获取当前位置
    getCurrentLocation(
        onLocationReceived = { lat, lng ->
            currentLat = lat
            currentLng = lng
            selectedLat = lat
            selectedLng = lng
            isLoadingLocation = false
            hasValidLocation = true
            Timber.d("📍 更新地图中心位置: lat=$lat, lng=$lng")
        },
        onLocationError = {
            Timber.w("无法获取当前位置")
            isLoadingLocation = false
            hasValidLocation = false
        }
    )
    
    // 初始化MapManager
    LaunchedEffect(key1 = context) {
        MapManager.initialize(context)
    }
    
    // 监听返回键
    BackHandler {
        onBackClick()
    }
    
    GeoTaskTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("选择位置") },
                    navigationIcon = {
                        IconButton(onClick = {
                            onBackClick()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 地图视图
                MapManager.MapView(
                    modifier = Modifier.fillMaxSize(),
                    initialLat = currentLat,
                    initialLng = currentLng,
                    onLocationSelected = { location, lat, lng ->
                        selectedLocation = location
                        selectedLat = lat
                        selectedLng = lng
                        isLocationConfirmed = true
                    },
                    context = context
                )
                
                // 位置确认提示
                if (isLocationConfirmed && selectedLocation.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "已选择位置",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedLocation,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isLocationConfirmed = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("重新选择")
                                }
                                Button(
                                    onClick = {
                                        onLocationSelected(selectedLocation, selectedLat, selectedLng)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("确认")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 地图选择Activity
 * 用于独立启动地图选择界面
 */
class MapPickerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GeoTaskTheme {
                MapPickerScreen(
                    onLocationSelected = { location, lat, lng ->
                        // 返回结果给调用者
                        intent.putExtra("selected_location", location)
                        intent.putExtra("selected_lat", lat)
                        intent.putExtra("selected_lng", lng)
                        setResult(RESULT_OK, intent)
                        finish()
                    },
                    onBackClick = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
    
    // 注意：地图生命周期现在由AMapProvider内部通过DisposableEffect自动管理
    // 不需要手动调用MapManager的生命周期方法
}
