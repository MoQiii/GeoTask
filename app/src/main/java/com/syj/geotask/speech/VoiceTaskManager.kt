package com.syj.geotask.speech

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.syj.geotask.utils.PermissionUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openapitools.client.apis.AiControllerApi
import org.openapitools.client.models.WorkflowRequest
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 语音任务管理器
 * 负责协调录音、语音识别和工作流调用，实现语音创建任务的完整流程
 */
class VoiceTaskManager(
    private val context: Context,
    private val speechToTextManager: SpeechToTextManager,
    private val aiControllerApi: AiControllerApi
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 录音状态 - 使用 StateFlow 而不是 MutableState
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // 错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 临时录音文件
    private var tempAudioFile: File? = null
    
    companion object {
        private const val WORKFLOW_ID = "create_task_from_voice"
        private const val AUDIO_FILE_PREFIX = "voice_recording_"
        private const val AUDIO_FILE_SUFFIX = ".wav"
    }
    
    /**
     * 初始化语音任务管理器
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 初始化语音识别管理器
            val speechInitialized = speechToTextManager.initialize()
            if (!speechInitialized) {
                Timber.e("语音识别管理器初始化失败")
                return@withContext false
            }
            
            Timber.d("语音任务管理器初始化成功")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "语音任务管理器初始化失败")
            return@withContext false
        }
    }
    
    /**
     * 开始录音
     */
    suspend fun startRecording(): Boolean = withContext(Dispatchers.IO) {
        Timber.d("VoiceTaskManager.startRecording() 被调用")
        try {
            if (_isRecording.value) {
                Timber.w("录音已在进行中")
                return@withContext false
            }
            
            // 检查录音权限
            if (!PermissionUtils.hasRecordAudioPermission(context)) {
                Timber.e("缺少录音权限")
                _errorMessage.value = "缺少录音权限，请在设置中允许录音权限"
                return@withContext false
            }
            
            // 检查位置权限
            if (!PermissionUtils.hasLocationPermission(context)) {
                Timber.w("缺少位置权限，将使用默认位置")
            }
            
            Timber.d("创建临时录音文件...")
            // 创建临时录音文件
            tempAudioFile = createTempAudioFile()
            Timber.d("临时录音文件创建完成: ${tempAudioFile?.absolutePath}")
            
            Timber.d("开始调用 speechToTextManager.startRecording()...")
            // 开始录音
            val success = speechToTextManager.startRecording(
                tempAudioFile!!,
                onError = { exception ->
                    Timber.e(exception, "录音失败")
                    _errorMessage.value = "录音失败: ${exception.message}"
                    _isRecording.value = false
                }
            )
            
            Timber.d("speechToTextManager.startRecording() 返回: $success")
            
            if (success) {
                _isRecording.value = true
                _errorMessage.value = null
                Timber.d("开始录音成功: ${tempAudioFile?.absolutePath}")
            } else {
                Timber.e("开始录音失败")
                _errorMessage.value = "开始录音失败"
            }
            
            return@withContext success
        } catch (e: Exception) {
            Timber.e(e, "开始录音时发生异常")
            _errorMessage.value = "开始录音失败: ${e.message}"
            return@withContext false
        }
    }
    
    /**
     * 停止录音并处理语音转文字和工作流调用
     * @param onSuccess 成功回调，返回创建的任务信息
     * @param onError 错误回调
     */
    suspend fun stopRecordingAndProcess(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            if (!_isRecording.value) {
                Timber.w("当前没有在录音")
                return@withContext
            }
            
            _isRecording.value = false
            _isProcessing.value = true
            _errorMessage.value = null
            
            // 停止录音
            speechToTextManager.stopRecording()
            Timber.d("录音已停止")
            
            // 转换语音为文字
            val recognizedText = transcribeAudio()
            if (recognizedText.isBlank()) {
                val errorMsg = "语音识别失败或未识别到内容"
                _errorMessage.value = errorMsg
                onError(errorMsg)
                _isProcessing.value = false
                return@withContext
            }
            
            Timber.d("语音识别结果: $recognizedText")
            
            // 调用工作流创建任务
            val workflowResult = callWorkflow(recognizedText)
            if (workflowResult.success == true) {
                Timber.d("工作流调用成功")
                onSuccess(recognizedText)
            } else {
                val errorMsg = workflowResult.errorMessage ?: "工作流调用失败"
                _errorMessage.value = errorMsg
                onError(errorMsg)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "处理录音失败")
            val errorMsg = "处理录音失败: ${e.message}"
            _errorMessage.value = errorMsg
            onError(errorMsg)
        } finally {
            _isProcessing.value = false
            // 清理临时文件
            cleanupTempFile()
        }
    }
    
    /**
     * 取消录音
     */
    suspend fun cancelRecording() = withContext(Dispatchers.IO) {
        try {
            if (_isRecording.value) {
                speechToTextManager.stopRecording()
                _isRecording.value = false
                Timber.d("录音已取消")
            }
            cleanupTempFile()
            _errorMessage.value = null
        } catch (e: Exception) {
            Timber.e(e, "取消录音失败")
        }
    }
    
    /**
     * 语音转文字
     */
    private suspend fun transcribeAudio(): String = withContext(Dispatchers.IO) {
        try {
            val audioFile = tempAudioFile
            if (audioFile == null || !audioFile.exists()) {
                Timber.e("录音文件不存在")
                return@withContext ""
            }
            
            val result = speechToTextManager.transcribeAudio(audioFile)
            val recognizedText = result.trim()
            
            // 获取当前位置信息
            val currentLocation = getCurrentLocation()
            val latitude = currentLocation?.latitude ?: 0.0
            val longitude = currentLocation?.longitude ?: 0.0
            
            // 计算当前时间两小时后的时间
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            val futureTime = calendar.time
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val futureTimeString = timeFormat.format(futureTime)
            
            // todo 暂时写死位置和时间的内容，后端工作流暂时没有设置处理时间和地理实体识别的功能
            val enhancedText = buildString {
                append("@任务文本@："+recognizedText)
                append("。")
                append("@其余内容@：")
                append(" isCompleted：未完成，")
                append(" latitude：$latitude，")
                append(" longitude：$longitude，")
                append(" geofenceRadius:200，")
                append(" isReminderEnabled：是，")
                append(" dueDate：\"$futureTimeString\"，")
                append(" dueTime：\"$futureTimeString\"")
            }
            
            Timber.d("原始识别文本: $recognizedText")
            Timber.d("增强后的文本: $enhancedText")
            
            return@withContext enhancedText
        } catch (e: Exception) {
            Timber.e(e, "语音转文字失败")
            return@withContext ""
        }
    }
    
    /**
     * 获取当前位置
     */
    private suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.Main) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // 检查位置权限
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            
            if (!isGpsEnabled && !isNetworkEnabled) {
                Timber.w("位置服务未启用")
                return@withContext null
            }
            
            // 尝试获取最后已知位置
            val location = when {
                isGpsEnabled -> {
                    try {
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } catch (e: SecurityException) {
                        Timber.e(e, "GPS位置权限不足")
                        null
                    }
                }
                isNetworkEnabled -> {
                    try {
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } catch (e: SecurityException) {
                        Timber.e(e, "网络位置权限不足")
                        null
                    }
                }
                else -> null
            }
            
            if (location != null) {
                Timber.d("获取到当前位置: 纬度=${location.latitude}, 经度=${location.longitude}")
            } else {
                Timber.w("无法获取当前位置，使用默认值")
            }
            
            return@withContext location
        } catch (e: Exception) {
            Timber.e(e, "获取位置信息失败")
            return@withContext null
        }
    }
    
    /**
     * 调用工作流
     */
    private suspend fun callWorkflow(recognizedText: String): org.openapitools.client.models.WorkflowResponse = withContext(Dispatchers.IO) {
        try {
            val workflowRequest = WorkflowRequest(
                inputs = mapOf(
                    "query" to recognizedText,
                    "text" to recognizedText
                ),
                workflowId = WORKFLOW_ID,
                streaming = false
            )
            
            Timber.d("调用工作流: $WORKFLOW_ID")
            Timber.d("输入参数: $recognizedText")
            
            // 调用工作流（超时已在OkHttpClient层面设置为15秒）
            val response = aiControllerApi.workflow(workflowRequest)
            
            Timber.d("工作流响应:")
            Timber.d("  成功: ${response.success}")
            Timber.d("  状态: ${response.status}")
            Timber.d("  执行ID: ${response.executionId}")
            Timber.d("  任务ID: ${response.taskId}")
            Timber.d("  输出: ${response.outputs}")
            
            return@withContext response
        } catch (e: Exception) {
            Timber.e(e, "调用工作流失败")
            return@withContext org.openapitools.client.models.WorkflowResponse(
                success = false,
                errorMessage = "调用工作流失败: ${e.message}"
            )
        }
    }
    
    /**
     * 创建临时录音文件
     */
    private fun createTempAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "$AUDIO_FILE_PREFIX$timestamp$AUDIO_FILE_SUFFIX"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * 清理临时文件
     */
    private fun cleanupTempFile() {
        try {
            tempAudioFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                    Timber.d("已删除临时录音文件: ${file.absolutePath}")
                }
            }
            tempAudioFile = null
        } catch (e: Exception) {
            Timber.e(e, "清理临时文件失败")
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        scope.launch {
            try {
                if (_isRecording.value) {
                    cancelRecording()
                }
                speechToTextManager.release()
                Timber.d("语音任务管理器资源已释放")
            } catch (e: Exception) {
                Timber.e(e, "释放资源失败")
            }
        }
    }
}
