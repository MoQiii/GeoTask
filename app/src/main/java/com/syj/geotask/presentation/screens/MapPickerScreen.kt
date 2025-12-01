package com.syj.geotask.presentation.screens

import android.os.Bundle
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.syj.geotask.presentation.map.MapManager
import com.syj.geotask.presentation.theme.GeoTaskTheme
import kotlinx.coroutines.launch

/**
 * 地图选择界面
 * 用于选择任务位置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLat: Double = 39.9042,
    initialLng: Double = 116.4074,
    onLocationSelected: (String, Double, Double) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedLocation by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLng by remember { mutableStateOf(initialLng) }
    var isLocationConfirmed by remember { mutableStateOf(false) }
    
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
                    initialLat = initialLat,
                    initialLng = initialLng,
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
        
        val initialLat = intent.getDoubleExtra("initial_lat", 39.9042)
        val initialLng = intent.getDoubleExtra("initial_lng", 116.4074)
        
        setContent {
            GeoTaskTheme {
                MapPickerScreen(
                    initialLat = initialLat,
                    initialLng = initialLng,
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
