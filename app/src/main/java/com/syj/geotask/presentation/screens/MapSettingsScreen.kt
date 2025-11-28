package com.syj.geotask.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syj.geotask.presentation.map.MapConfig
import com.syj.geotask.presentation.map.MapManager

/**
 * 地图设置界面
 * 用于配置地图相关设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProvider = remember { MapManager.getCurrentProviderName() }
    val isConfigured = remember { MapManager.isProviderConfigured(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("地图设置") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 当前地图提供者信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "当前地图服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "高德地图",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isConfigured) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已配置",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已配置",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "需要配置",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "需要配置API密钥",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配置说明
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "配置说明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. 访问高德开放平台 (lbs.amap.com)\n" +
                          "2. 注册账号并创建应用\n" +
                          "3. 获取API密钥\n" +
                          "4. 在 MapConfig.kt 中配置密钥\n" +
                          "5. 在 AndroidManifest.xml 中同步配置",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 功能特性
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "功能特性",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val features = listOf(
                    "🗺️ 真实地图显示",
                    "📍 点击选择位置",
                    "🎯 拖拽标记调整",
                    "📝 自动地址解析",
                    "🔍 精确定位"
                )
                
                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "状态信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "地图提供者: $currentProvider\n" +
                          "初始化状态: ${if (MapManager.isCurrentProviderInitialized()) "已初始化" else "未初始化"}\n" +
                          "API密钥状态: ${if (isConfigured) "已配置" else "未配置"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
