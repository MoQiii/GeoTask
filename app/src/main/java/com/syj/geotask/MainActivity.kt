package com.syj.geotask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.syj.geotask.presentation.map.MapManager
import com.syj.geotask.presentation.screens.AboutScreen
import com.syj.geotask.presentation.screens.AddTaskScreen
import com.syj.geotask.presentation.screens.EditTaskScreen
import com.syj.geotask.presentation.screens.MapPickerScreen
import com.syj.geotask.presentation.screens.SettingsScreen
import com.syj.geotask.presentation.screens.TaskDetailScreen
import com.syj.geotask.presentation.screens.TaskListScreen
import com.syj.geotask.presentation.theme.GeoTaskTheme
import com.syj.geotask.presentation.theme.ThemeManager
import com.syj.geotask.utils.LogTest
import com.syj.geotask.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查和记录权限状态
        checkAndLogPermissions()
        
        // 初始化地图管理器
        MapManager.initialize(this)
        
        // 测试Timber日志功能
        Timber.d("MainActivity onCreate - 开始测试Timber日志")
        Timber.i("应用启动完成")
        Timber.w("这是一条警告日志")
        Timber.e("这是一条错误日志")
        
        // 测试LogTest工具类
        LogTest.testLogging(this)
        
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            
            // 监听主题变化
            LaunchedEffect(Unit) {
                themeManager.darkModeFlow.collect { isDarkMode ->
                    darkMode = isDarkMode
                }
            }
            
            GeoTaskTheme(
                darkTheme = darkMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "task_list"
                    ) {
                        composable("task_list") {
                            TaskListScreen(
                                onNavigateToAddTask = {
                                    navController.navigate("add_task")
                                },
                                onNavigateToTaskDetail = { taskId ->
                                    navController.navigate("task_detail/$taskId")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("add_task") {
                            AddTaskScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToMapPicker = {
                                    navController.navigate("map_picker?source=add_task")
                                },
                                navController = navController
                            )
                        }
                        //需要一个任务id参数 {taskId}占位符
                        composable("task_detail/{taskId}") { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull() ?: return@composable
                            TaskDetailScreen(
                                taskId = taskId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToEdit = { taskId ->
                                    navController.navigate("edit_task/$taskId")
                                }
                            )
                        }

                        composable("edit_task/{taskId}") { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull() ?: return@composable
                            EditTaskScreen(
                                taskId = taskId,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToMapPicker = {
                                    navController.navigate("map_picker?source=edit_task")
                                },
                                navController = navController
                            )
                        }

                        composable(
                            "map_picker?source={source}",
                            arguments = listOf(
                                navArgument("source") {
                                    type = NavType.StringType
                                    defaultValue = "unknown"
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val source = backStackEntry.arguments?.getString("source") ?: "unknown"
                            MapPickerScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onLocationSelected = { location, lat, lng ->
                                    // 根据来源返回到相应的页面，并传递位置数据
                                    when (source) {
                                        "add_task" -> {
                                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "selected_location", 
                                                Triple(location, lat, lng)
                                            )
                                        }
                                        "edit_task" -> {
                                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "selected_location", 
                                                Triple(location, lat, lng)
                                            )
                                        }
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToAbout = {
                                    navController.navigate("about")
                                },
                                themeManager = themeManager
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun checkAndLogPermissions() {
        // 检查位置权限
        val fineLocationGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        Timber.d("权限检查 - 精确位置权限: $fineLocationGranted")
        Timber.d("权限检查 - 大致位置权限: $coarseLocationGranted")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            Timber.d("权限检查 - 后台位置权限: $backgroundLocationGranted")
        }
        
        // 检查其他相关权限
        val networkStateGranted = checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
        val wifiStateGranted = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        
        Timber.d("权限检查 - 网络状态权限: $networkStateGranted")
        Timber.d("权限检查 - WiFi状态权限: $wifiStateGranted")
        
        // 记录Android版本信息
        Timber.d("系统信息 - Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Timber.d("系统信息 - 设备型号: ${Build.MODEL}")
        Timber.d("系统信息 - 制造商: ${Build.MANUFACTURER}")
        
        // 如果权限不足，记录警告
        if (!fineLocationGranted || !coarseLocationGranted) {
            Timber.w("警告: 位置权限未完全授权，可能影响地图功能")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 检查是否正在真正退出应用（而不是配置变更）
        if (isFinishing) {
            MapManager.destroyCompletely()
        }
        // 注意：地图生命周期现在由AMapProvider内部自动管理，不需要手动调用
    }
}
