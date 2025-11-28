package com.syj.geotask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syj.geotask.presentation.map.MapManager
import com.syj.geotask.presentation.screens.AboutScreen
import com.syj.geotask.presentation.screens.AddTaskScreen
import com.syj.geotask.presentation.screens.EditTaskScreen
import com.syj.geotask.presentation.screens.MapPickerScreen
import com.syj.geotask.presentation.screens.SettingsScreen
import com.syj.geotask.presentation.screens.TaskDetailScreen
import com.syj.geotask.presentation.screens.TaskListScreen
import com.syj.geotask.presentation.theme.GeoTaskTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化地图管理器
        MapManager.initialize(this)
        
        setContent {
            GeoTaskTheme {
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
                                    navController.navigate("map_picker")
                                }
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
                                    navController.navigate("map_picker")
                                }
                            )
                        }

                        composable("map_picker") {
                            MapPickerScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onLocationSelected = { location, lat, lng ->
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
                                }
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
    
    override fun onResume() {
        super.onResume()
        MapManager.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        MapManager.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        MapManager.onDestroy()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        MapManager.onLowMemory()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        MapManager.onSaveInstanceState(outState)
    }
}
