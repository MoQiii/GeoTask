package com.syj.geotask.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.syj.geotask.domain.model.Task
import com.syj.geotask.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMapPicker: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
    navController: NavController
) {
    // 获取当前的NavController来监听返回的位置数据
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // 使用ViewModel中的状态，而不是本地的remember状态
    val title = viewModel.taskTitle
    val description = viewModel.taskDescription
    val selectedDate = viewModel.selectedDate
    val selectedTime = viewModel.selectedTime
    val isReminderEnabled = viewModel.isReminderEnabled
    val selectedLocation = viewModel.selectedLocation
    val selectedLatitude = viewModel.selectedLatitude
    val selectedLongitude = viewModel.selectedLongitude
    val geofenceRadius = viewModel.geofenceRadius
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var shouldSave by remember { mutableStateOf(false) }

    // 监听从MapPickerScreen返回的位置数据
    LaunchedEffect(Unit) {
        // 使用一个标志来确保只监听一次
        var hasProcessed = false
        
        while (!hasProcessed) {
            currentBackStackEntry?.let { backStackEntry ->
                // 使用 get 方法获取数据
                val locationData = backStackEntry.savedStateHandle.get<Triple<String, Double, Double>>("selected_location")
                locationData?.let { (location, lat, lng) ->
                    // 更新ViewModel中的位置状态
                    viewModel.updateSelectedLocation(location, lat, lng)
                    // 清除已处理的数据
                    backStackEntry.savedStateHandle.remove<Triple<String, Double, Double>>("selected_location")
                    hasProcessed = true
                }
            }
            kotlinx.coroutines.delay(100) // 每100ms检查一次
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加任务") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTaskTitle(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题 *") },
                singleLine = true,
                isError = title.isBlank()
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.updateTaskDescription(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("描述") },
                maxLines = 5
            )

            // Date Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日期:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = { showDatePicker = true }
                ) {
                    Text(formatDate(selectedDate))
                }
            }

            // Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "时间:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = { showTimePicker = true }
                ) {
                    Text(formatTime(selectedTime))
                }
            }

            // Reminder Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "启用提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { viewModel.updateReminderEnabled(it) }
                )
            }

            // Location Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "位置:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = selectedLocation ?: "未设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedLocation != null) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onNavigateToMapPicker
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("选择地点")
                }
            }

            // Geofence Radius Input
            var radiusText by remember { mutableStateOf("") }
            
            LaunchedEffect(geofenceRadius) {
                radiusText = if (geofenceRadius == 200f) "" else geofenceRadius.toString()
            }
            
            OutlinedTextField(
                value = radiusText,
                onValueChange = { 
                    radiusText = it
                    val radius = it.toFloatOrNull()
                    if (it.isEmpty()) {
                        viewModel.updateGeofenceRadius(200f) // 默认值
                    } else if (radius != null && radius > 0) {
                        viewModel.updateGeofenceRadius(radius)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地理围栏半径 (米)") },
                placeholder = { Text("默认: 200米") },
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    shouldSave = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("保存任务")
            }

            // 处理保存任务的协程
            LaunchedEffect(shouldSave) {
                if (shouldSave) {
                    val success = viewModel.saveTask()
                    if (success) {
                        // 保存成功后才返回
                        onNavigateBack()
                    }
                    shouldSave = false // 重置状态
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                viewModel.updateSelectedDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            initialDate = selectedDate
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            onTimeSelected = { time ->
                viewModel.updateSelectedTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            initialTime = selectedTime
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    initialDate: Date
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onTimeSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    initialTime: Date
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hours,
        initialMinute = initialTime.minutes,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    onTimeSelected(calendar.time)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatDate(date: Date): String {
    val format = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    return format.format(date)
}

private fun formatTime(date: Date): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}
