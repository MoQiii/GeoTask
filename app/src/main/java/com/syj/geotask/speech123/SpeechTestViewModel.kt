package com.syj.geotask.speech123

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SpeechTestUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val recognizedText: String = "",
    val errorMessage: String? = null,
    val isInitialized: Boolean = false
)

@HiltViewModel
class SpeechTestViewModel @Inject constructor(
    private val speechToTextManager: SpeechToTextManager
) : ViewModel() {
    
    private val TAG = "SpeechTestViewModel"
    
    private val _uiState = MutableStateFlow(SpeechTestUiState())
    val uiState: StateFlow<SpeechTestUiState> = _uiState.asStateFlow()
    
    private var audioFile: File? = null
    
    init {
        initializeSpeechToText()
    }
    
    /**
     * 初始化语音转文本功能
     */
    private fun initializeSpeechToText() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                val success = speechToTextManager.initialize()
                _uiState.value = _uiState.value.copy(
                    isInitialized = success,
                    isProcessing = false,
                    errorMessage = if (!success) "语音识别初始化失败" else null
                )
                
                if (success) {
                    Log.d(TAG, "语音转文本功能初始化成功")
                } else {
                    Log.e(TAG, "语音转文本功能初始化失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "初始化语音转文本功能时发生错误", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "初始化失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 开始或停止录音
     */
    fun toggleRecording() {
        val currentState = _uiState.value
        
        if (currentState.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    /**
     * 开始录音
     */
    private fun startRecording() {
        if (!_uiState.value.isInitialized) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "语音识别未初始化"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                // 创建临时音频文件
                audioFile = File.createTempFile("recording_", ".wav", File("/data/data/com.syj.geotask/cache"))
                
                val success = speechToTextManager.startRecording(
                    audioFile!!,
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            isRecording = false,
                            errorMessage = "录音失败: ${error.message}"
                        )
                    }
                )
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isRecording = true,
                        errorMessage = null,
                        recognizedText = ""
                    )
                    Log.d(TAG, "开始录音")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "启动录音失败"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "开始录音时发生错误", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "录音失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 停止录音并转录
     */
    private fun stopRecording() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                
                // 停止录音
                speechToTextManager.stopRecording()
                Log.d(TAG, "录音已停止")
                
                _uiState.value = _uiState.value.copy(isRecording = false)
                
                // 转录音频
                audioFile?.let { file ->
                    if (file.exists()) {
                        val text = speechToTextManager.transcribeAudio(file)
                        _uiState.value = _uiState.value.copy(
                            recognizedText = text,
                            isProcessing = false,
                            errorMessage = null
                        )
                        Log.d(TAG, "转录完成: $text")
                        
                        // 清理临时文件
                        file.delete()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = "录音文件不存在"
                        )
                    }
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "录音文件为空"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "停止录音或转录时发生错误", e)
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    isProcessing = false,
                    errorMessage = "处理失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 清除识别的文本
     */
    fun clearText() {
        _uiState.value = _uiState.value.copy(recognizedText = "")
    }
    
    /**
     * 获取系统信息
     */
    fun getSystemInfo() {
        viewModelScope.launch {
            try {
                val info = speechToTextManager.getSystemInfo()
                Log.d(TAG, "系统信息: $info")
            } catch (e: Exception) {
                Log.e(TAG, "获取系统信息失败", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                speechToTextManager.release()
            } catch (e: Exception) {
                Log.e(TAG, "释放资源时发生错误", e)
            }
        }
    }
}
