package com.syj.geotask.presentation.map

import android.content.Context
import timber.log.Timber
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.syj.geotask.utils.PermissionUtils
import com.syj.geotask.utils.RequestMapPermissions
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult

/**
 * 高德地图提供者实现
 * 使用高德地图SDK提供完整的地图功能
 */
class AMapProvider : MapProvider {
    
    private var isInitialized = false
    private var apiKey: String = ""
    private var context: Context? = null
    private var mapView: MapView? = null
    private var aMap: AMap? = null
    private var marker: Marker? = null
    private var geocodeSearch: GeocodeSearch? = null
    
    override fun getProviderName(): String = "AMap"
    
    override fun initialize(context: Context, apiKey: String) {
        // 保存context引用
        this.context = context.applicationContext
        
        // 从MapConfig获取API密钥，确保从AndroidManifest.xml中读取
        this.apiKey = MapConfig.getApiKey(context, "AMap")
        
        Timber.d("读取到的API密钥: '${this.apiKey}'")
        Timber.d("API密钥长度: ${this.apiKey.length}")
        Timber.d("API密钥是否为空: ${this.apiKey.isEmpty()}")
        
        // 检查API密钥是否已配置
        val isConfigured = MapConfig.isApiKeyConfigured(context, "AMap")
        Timber.d("API密钥配置状态: $isConfigured")
        
        if (!isConfigured) {
            Timber.e("高德地图API密钥未配置或无效")
            isInitialized = false
            return
        }
        
        try {
            // 设置隐私合规 - 必须在使用SDK任何接口前调用
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(context, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(context, true)
            Timber.d("高德地图隐私合规设置完成")
            
            // 高德地图SDK会自动从AndroidManifest中读取API密钥
            // 这里我们只需要标记为已初始化
            isInitialized = true
            Timber.d("高德地图提供者初始化成功，API密钥: ${this.apiKey.take(8)}...")
        } catch (e: Exception) {
            Timber.e(e, "高德地图提供者初始化失败")
            isInitialized = false
        }
    }
    
    override fun isInitialized(): Boolean = isInitialized
    
    @Composable
    override fun MapView(
        modifier: Modifier,
        initialLat: Double,
        initialLng: Double,
        onLocationSelected: (String, Double, Double) -> Unit,
        apiKey: String
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        
        // 状态管理
        var selectedAddress by remember { mutableStateOf("正在获取地址...") }
        var isLoading by remember { mutableStateOf(false) }
        var mapReady by remember { mutableStateOf(false) }
        
        // 初始化地图提供者
        LaunchedEffect(key1 = context) {
            if (!isInitialized) {
                initialize(context, apiKey)
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            // 检查权限和API密钥
            val hasPermissions = PermissionUtils.canUseMapFeatures(context)
            val apiKeyConfigured = MapConfig.isApiKeyConfigured(context, "AMap")
            
            if (!apiKeyConfigured) {
                // 显示API密钥错误提示
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "错误",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "地图配置错误",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "高德地图API密钥未配置或无效",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请在AndroidManifest.xml中配置有效的API密钥",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!hasPermissions) {
                // 自动申请权限
                var permissionRequested by remember { mutableStateOf(false) }
                var permissionGranted by remember { mutableStateOf(false) }
                
                // 自动触发权限申请
                if (!permissionRequested) {
                    RequestMapPermissions { granted ->
                        permissionRequested = true
                        permissionGranted = granted
                        if (granted) {
                            // 权限申请成功，重新初始化地图
                            initialize(context, apiKey)
                        }
                    }
                }
                
                // 显示权限申请中或申请失败的界面
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!permissionRequested) {
                        // 权限申请中
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在申请权限...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请允许位置权限和网络权限以使用地图功能",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!permissionGranted) {
                        // 权限申请被拒绝
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = "权限被拒绝",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "权限申请被拒绝",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "需要位置权限和网络权限才能使用地图功能",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val missingPermissions = PermissionUtils.getMissingPermissions(context)
                        Text(
                            text = "缺失权限: ${missingPermissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 重新申请权限按钮
                        var retryRequest by remember { mutableStateOf(false) }
                        
                        if (retryRequest) {
                            RequestMapPermissions { granted ->
                                retryRequest = false
                                permissionGranted = granted
                                if (granted) {
                                    // 权限申请成功，重新初始化地图
                                    initialize(context, apiKey)
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                retryRequest = true
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("重新申请权限")
                        }
                    }
                }
            } else {
                // 高德地图视图
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            mapView = this
                            onCreate(null)
                            
                            // 获取地图控制器 - 尝试直接获取
                            try {
                                aMap = this.map
                                setupMap(aMap!!, initialLat, initialLng, onLocationSelected) { address ->
                                    selectedAddress = address
                                    isLoading = false
                                }
                                
                                // 初始化地理编码搜索
                                initializeGeocodeSearch(ctx) { address ->
                                    selectedAddress = address
                                    isLoading = false
                                }
                                
                                mapReady = true
                            } catch (e: Exception) {
                                Timber.e(e, "地图初始化失败")
                                selectedAddress = "地图初始化失败"
                            }
                        }
                    },
                    update = { view ->
                        // 更新地图视图
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 生命周期管理
                DisposableEffect(lifecycleOwner) {
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                Timber.d("MapView onResume")
                                mapView?.onResume()
                            }
                            Lifecycle.Event.ON_PAUSE -> {
                                Timber.d("MapView onPause")
                                mapView?.onPause()
                            }
                            Lifecycle.Event.ON_DESTROY -> {
                                Timber.d("MapView onDestroy")
                                mapView?.onDestroy()
                                mapView = null
                                aMap = null
                                marker = null
                                geocodeSearch = null
                            }
                            else -> {}
                        }
                    }
                    
                    // 添加生命周期观察者
                    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                    //用于释放资源
                    onDispose {
                        // 移除生命周期观察者
                        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                        // 确保清理资源
                        mapView?.onDestroy()
                        mapView = null
                        aMap = null
                        marker = null
                        geocodeSearch = null
                    }
                }
                
                // 底部信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "选中位置",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                                Text(
                                    text = "坐标: ${String.format("%.6f", initialLat)}, ${String.format("%.6f", initialLng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                if (mapReady && selectedAddress != "正在获取地址..." && selectedAddress != "地址解析失败") {
                                    onLocationSelected(selectedAddress, initialLat, initialLng)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && mapReady
                        ) {
                            Text("确认位置")
                        }
                    }
                }
                
                // 加载指示器
                if (!mapReady) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "正在加载地图...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 初始化地理编码搜索
     */
    private fun initializeGeocodeSearch(ctx: Context, onAddressUpdate: (String) -> Unit) {
        try {
            geocodeSearch = GeocodeSearch(ctx)
            Timber.d("地理编码搜索实例创建成功")
            
            // 注意：监听器将在每次查询时单独设置，确保回调正确
            Timber.d("地理编码搜索初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "地理编码搜索初始化失败")
            onAddressUpdate("地理编码服务初始化失败")
        }
    }
    
    /**
     * 设置地图
     */
    private fun setupMap(
        map: AMap,
        initialLat: Double,
        initialLng: Double,
        onLocationSelected: (String, Double, Double) -> Unit,
        onAddressUpdate: (String) -> Unit
    ) {
        try {
            // 设置地图UI设置
            map.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = true
                isScaleControlsEnabled = true
            }
            
            // 设置地图类型
            map.mapType = AMap.MAP_TYPE_NORMAL
            
            // 启用定位
            map.isMyLocationEnabled = true
            
            // 设置初始位置
            val initialPosition = LatLng(initialLat, initialLng)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15f))
            
            // 添加标记
            marker = map.addMarker(MarkerOptions().apply {
                position(initialPosition)
                draggable(true)
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                title("选中位置")
            })
            
            // 地图点击事件
            map.setOnMapClickListener { latLng ->
                marker?.position = latLng
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                
                // 反向地理编码
                onAddressUpdate("正在获取地址...")
                reverseGeocode(latLng.latitude, latLng.longitude) { address ->
                    onAddressUpdate(address)
                }
            }
            
            // 地图长按事件 - 用于快速选择位置
            map.setOnMapLongClickListener { latLng ->
                marker?.position = latLng
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                
                // 反向地理编码
                onAddressUpdate("正在获取地址...")
                reverseGeocode(latLng.latitude, latLng.longitude) { address ->
                    onAddressUpdate(address)
                    // 长按后自动触发位置选择
                    onLocationSelected(address, latLng.latitude, latLng.longitude)
                }
            }
            
            // 标记拖拽事件
            map.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                
                override fun onMarkerDrag(marker: Marker) {}
                
                override fun onMarkerDragEnd(marker: Marker) {
                    val position = marker.position
                    map.animateCamera(CameraUpdateFactory.newLatLng(position))
                    
                    // 反向地理编码
                    onAddressUpdate("正在获取地址...")
                    reverseGeocode(position.latitude, position.longitude) { address ->
                        onAddressUpdate(address)
                    }
                }
            })
            
            // 初始地址解析
            onAddressUpdate("正在获取地址...")
            reverseGeocode(initialLat, initialLng) { address ->
                onAddressUpdate(address)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "设置地图失败")
            onAddressUpdate("地图初始化失败")
        }
    }
    
    /**
     * 反向地理编码
     */
    private fun reverseGeocode(lat: Double, lng: Double, onResult: (String) -> Unit) {
        try {
            Timber.d("开始反向地理编码: lat=$lat, lng=$lng")
            
            // 如果geocodeSearch未初始化，尝试重新初始化
            if (geocodeSearch == null) {
                Timber.w("GeocodeSearch未初始化，尝试重新初始化")
                val ctx = this.context
                if (ctx == null) {
                    Timber.e("Context为空，无法初始化地理编码服务")
                    onResult("地址解析服务未初始化，请重启应用")
                    return
                }
                
                // 重新初始化地理编码服务
                initializeGeocodeSearch(ctx) { address ->
                    // 初始化完成后，继续执行反向地理编码
                    if (address.contains("初始化失败") || address.contains("未初始化")) {
                        onResult(address)
                        return@initializeGeocodeSearch
                    }
                    
                    // 初始化成功，执行反向地理编码
                    performReverseGeocode(lat, lng, onResult)
                }
                return
            }
            
            // 直接执行反向地理编码
            performReverseGeocode(lat, lng, onResult)
            
        } catch (e: Exception) {
            Timber.e(e, "反向地理编码异常")
            onResult("地址解析异常: ${e.message}")
        }
    }
    
    /**
     * 执行反向地理编码查询
     */
    private fun performReverseGeocode(lat: Double, lng: Double, onResult: (String) -> Unit) {
        try {
            Timber.d("准备执行反向地理编码: lat=$lat, lng=$lng")
            
            // 检查geocodeSearch是否可用
            val search = geocodeSearch
            if (search == null) {
                Timber.e("GeocodeSearch实例为null")
                onResult("地理编码服务不可用")
                return
            }
            
            val query = RegeocodeQuery(
                LatLonPoint(lat, lng),
                200f,
                GeocodeSearch.AMAP
            )
            
            Timber.d("创建查询对象成功: $query")
            
            // 设置监听器并执行查询
            search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                    Timber.d("反向地理编码回调触发: code=$code")
                    Timber.d("回调结果: result=$result")
                    
                    if (code == 1000) {
                        Timber.d("反向地理编码成功")
                        val address = result?.regeocodeAddress?.formatAddress
                        if (!address.isNullOrEmpty()) {
                            Timber.d("地址解析成功: $address")
                            onResult(address)
                        } else {
                            Timber.w("地址解析结果为空，尝试构建简单地址")
                            // 尝试获取更详细的地址信息
                            val simpleAddress = result?.regeocodeAddress?.let { regeocodeAddress ->
                                val province = regeocodeAddress.province ?: ""
                                val city = regeocodeAddress.city ?: ""
                                val district = regeocodeAddress.district ?: ""
                                val township = regeocodeAddress.township ?: ""
                                val street = regeocodeAddress.streetNumber?.street ?: ""
                                
                                val fullAddress = listOf(province, city, district, township, street)
                                    .filter { it.isNotEmpty() }
                                    .joinToString("")
                                
                                Timber.d("构建的简单地址: $fullAddress")
                                fullAddress.ifEmpty { "未知地址" }
                            } ?: "未知地址"
                            onResult(simpleAddress)
                        }
                    } else {
                        Timber.e("反向地理编码失败: $code")
                        val errorMessage = when (code) {
                            1001 -> "API密钥错误，请检查配置"
                            1002 -> "请求参数非法"
                            1003 -> "网络连接失败，请检查网络"
                            1004 -> "网络超时，请重试"
                            1005 -> "解析失败，请稍后重试"
                            2000 -> "请求参数非法"
                            2001 -> "API密钥错误，请检查配置"
                            2002 -> "权限不足，服务被拒绝"
                            2003 -> "配额超限，访问频率受限"
                            else -> "地址解析失败 (错误码: $code)"
                        }
                        Timber.e("错误信息: $errorMessage")
                        onResult(errorMessage)
                    }
                }
                
                override fun onGeocodeSearched(result: GeocodeResult?, code: Int) {
                    // 正向地理编码结果，这里不需要
                    Timber.d("正向地理编码回调: code=$code")
                }
            })
            
            Timber.d("监听器设置完成，开始执行查询")
            
            // 执行异步查询
            val resultCode = search.getFromLocationAsyn(query)
            Timber.d("查询已发送，返回码: $resultCode")
            
            // 设置超时处理
            var isTimeout = false
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isTimeout) {
                    isTimeout = true
                    Timber.w("地址查询超时，可能是网络问题")
                    onResult("地址查询超时，请检查网络连接")
                }
            }, 10000) // 10秒超时
            
        } catch (e: Exception) {
            Timber.e(e, "执行反向地理编码异常")
            onResult("地址解析异常: ${e.message}")
        }
    }
    
    /**
     * 处理地图生命周期
     */
    fun onResume() {
        mapView?.onResume()
    }
    
    fun onPause() {
        mapView?.onPause()
    }
    
    fun onDestroy() {
        mapView?.onDestroy()
        mapView = null
        aMap = null
        marker = null
        geocodeSearch = null
        // 不要重置isInitialized状态，保持provider可用
        // isInitialized = false
    }
    
    fun onLowMemory() {
        mapView?.onLowMemory()
    }
    
    fun onSaveInstanceState(outState: android.os.Bundle) {
        mapView?.onSaveInstanceState(outState)
    }
}
