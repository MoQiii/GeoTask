package com.syj.geotask.presentation.map

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
    private var mapView: MapView? = null
    private var aMap: AMap? = null
    private var marker: Marker? = null
    private var geocodeSearch: GeocodeSearch? = null
    
    override fun getProviderName(): String = "AMap"
    
    override fun initialize(context: Context, apiKey: String) {
        // 从MapConfig获取API密钥，确保从AndroidManifest.xml中读取
        this.apiKey = MapConfig.getApiKey(context, "AMap")
        
        Log.d("AMapProvider", "读取到的API密钥: '${this.apiKey}'")
        Log.d("AMapProvider", "API密钥长度: ${this.apiKey.length}")
        Log.d("AMapProvider", "API密钥是否为空: ${this.apiKey.isEmpty()}")
        
        // 检查API密钥是否已配置
        val isConfigured = MapConfig.isApiKeyConfigured(context, "AMap")
        Log.d("AMapProvider", "API密钥配置状态: $isConfigured")
        
        if (!isConfigured) {
            Log.e("AMapProvider", "高德地图API密钥未配置或无效")
            isInitialized = false
            return
        }
        
        try {
            // 设置隐私合规 - 必须在使用SDK任何接口前调用
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(context, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(context, true)
            Log.d("AMapProvider", "高德地图隐私合规设置完成")
            
            // 高德地图SDK会自动从AndroidManifest中读取API密钥
            // 这里我们只需要标记为已初始化
            isInitialized = true
            Log.d("AMapProvider", "高德地图提供者初始化成功，API密钥: ${this.apiKey.take(8)}...")
        } catch (e: Exception) {
            Log.e("AMapProvider", "高德地图提供者初始化失败", e)
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
            // 检查API密钥是否有效
            if (!MapConfig.isApiKeyConfigured(context, "AMap")) {
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
                                Log.e("AMapProvider", "地图初始化失败", e)
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
                                Log.d("AMapProvider", "MapView onResume")
                                mapView?.onResume()
                            }
                            Lifecycle.Event.ON_PAUSE -> {
                                Log.d("AMapProvider", "MapView onPause")
                                mapView?.onPause()
                            }
                            Lifecycle.Event.ON_DESTROY -> {
                                Log.d("AMapProvider", "MapView onDestroy")
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
        geocodeSearch = GeocodeSearch(ctx).apply {
            setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                    if (code == 1000) {
                        val address = result?.regeocodeAddress?.formatAddress
                        if (!address.isNullOrEmpty()) {
                            onAddressUpdate(address)
                        }
                    } else {
                        Log.e("AMapProvider", "反向地理编码失败: $code")
                        onAddressUpdate("地址解析失败")
                    }
                }
                
                override fun onGeocodeSearched(result: GeocodeResult?, code: Int) {
                    // 正向地理编码结果，这里不需要
                }
            })
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
            Log.e("AMapProvider", "设置地图失败", e)
            onAddressUpdate("地图初始化失败")
        }
    }
    
    /**
     * 反向地理编码
     */
    private fun reverseGeocode(lat: Double, lng: Double, onResult: (String) -> Unit) {
        try {
            val query = RegeocodeQuery(
                LatLonPoint(lat, lng),
                200f,
                GeocodeSearch.AMAP
            )
            
            // 创建临时的GeocodeSearch来处理单次请求
            val tempGeocodeSearch = GeocodeSearch(mapView?.context).apply {
                setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                    override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                        if (code == 1000) {
                            val address = result?.regeocodeAddress?.formatAddress
                            if (!address.isNullOrEmpty()) {
                                onResult(address)
                            } else {
                                onResult("未知地址")
                            }
                        } else {
                            Log.e("AMapProvider", "反向地理编码失败: $code")
                            onResult("地址解析失败")
                        }
                    }
                    
                    override fun onGeocodeSearched(result: GeocodeResult?, code: Int) {
                        // 正向地理编码结果，这里不需要
                    }
                })
            }
            
            tempGeocodeSearch.getFromLocationAsyn(query)
        } catch (e: Exception) {
            Log.e("AMapProvider", "反向地理编码失败", e)
            onResult("地址解析失败")
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
