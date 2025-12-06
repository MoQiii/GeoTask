package com.syj.geotask.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App图标和名称
            Text(
                text = "GeoTask",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "地理位置提醒任务管理",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 版本信息
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "版本信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "版本: 0.1",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "构建: 2025.12",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 功能特性
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "主要功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• 任务创建与管理",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 地理位置提醒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 时间提醒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 地图选点",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 深色模式",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 技术栈
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "技术栈",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• Jetpack Compose",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• MVVM + Repository",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Room 数据库",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Hilt 依赖注入",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• WorkManager",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Google Maps",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "© 2025 GeoTask\n一个展示Android开发技能的项目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
