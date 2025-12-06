package com.syj.geotask.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syj.geotask.domain.model.Task
import com.syj.geotask.presentation.viewmodel.FilterType
import com.syj.geotask.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToAddTask: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSpeechTest: () -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVoiceRecording by viewModel.isVoiceRecording.collectAsState()
    val isVoiceProcessing by viewModel.isVoiceProcessing.collectAsState()
    val voiceErrorMessage by viewModel.voiceErrorMessage.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        if (searchQuery != viewModel.searchQuery) {
            viewModel.onSearchQueryChanged(searchQuery)
        }
    }

    LaunchedEffect(filterType) {
        if (filterType != viewModel.filterType) {
            viewModel.onFilterTypeChanged(filterType)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onSearchQueryChanged(searchQuery)
    }

    voiceErrorMessage?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            delay(3000)
            viewModel.clearVoiceError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoTask") },
                actions = {
                    IconButton(onClick = onNavigateToSpeechTest) {
                        Icon(Icons.Default.Mic, contentDescription = "语音测试")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Timber.d("🔘 新增任务按钮点击触发")
                    onNavigateToAddTask()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isVoiceRecording || isVoiceProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVoiceRecording)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVoiceRecording) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "录音中",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "正在录音... 点击按钮结束",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "正在处理语音...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                voiceErrorMessage?.let { errorMsg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearVoiceError() }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索任务...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterType.values().forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(text = getFilterTypeName(type)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks) { task ->
                            TaskItemCard(
                                task = task,
                                onClick = { onNavigateToTaskDetail(task.id) },
                                onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                onToggleReminder = { enabled -> 
                                    viewModel.toggleTaskReminder(task.id, enabled) 
                                }
                            )
                        }
                    }
                }
            }
            
            FloatingActionButton(
                onClick = {
                    if (isVoiceRecording || isVoiceProcessing) {
                        scope.launch {
                            if (isVoiceRecording) {
                                Timber.d("🛑 停止语音录音")
                                viewModel.stopVoiceRecordingAndProcess(
                                    onSuccess = { recognizedText ->
                                        Timber.d("✅ 语音任务创建成功: $recognizedText")
                                    },
                                    onError = { errorMsg ->
                                        Timber.e("❌ 语音任务创建失败: $errorMsg")
                                    }
                                )
                            }
                        }
                    } else {
                        scope.launch {
                            Timber.d("🎤 开始语音录音")
                            val recordingStarted = viewModel.startVoiceRecording()
                            Timber.d("🎤 语音录音启动结果: $recordingStarted")
                            if (!recordingStarted) {
                                Timber.e("❌ 录音启动失败")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = if (isVoiceRecording) 
                    MaterialTheme.colorScheme.error 
                else if (isVoiceProcessing)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.secondary
            ) {
                if (isVoiceProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = if (isVoiceRecording) "停止录音" else "语音录制",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: Task,
    onClick: () -> Unit,
    onToggleCompletion: () -> Unit,
    onToggleReminder: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompletion() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateTime(task.dueDate, task.dueTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.location != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "有位置提醒",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (task.isReminderEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AccessAlarm,
                            contentDescription = "已启用提醒",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Switch(
                checked = task.isReminderEnabled,
                onCheckedChange = onToggleReminder
            )
        }
    }
}

private fun getFilterTypeName(filterType: FilterType): String {
    return when (filterType) {
        FilterType.ALL -> "全部"
        FilterType.COMPLETED -> "已完成"
        FilterType.INCOMPLETE -> "未完成"
    }
}

private fun formatDateTime(date: Long, time: Long): String {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val dateStr = dateFormat.format(Date(date))
    val timeStr = timeFormat.format(Date(time))
    
    return "$dateStr $timeStr"
}
