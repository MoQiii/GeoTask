package com.syj.geotask.speech123

import android.content.Context
import android.util.Log
import com.syj.geotask.media.decodeWaveFile
import com.syj.geotask.recorder.Recorder
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class SpeechToTextManager(private val context: Context) {
    private val TAG = "SpeechToTextManager"
    private var whisperContext: WhisperContext? = null
    private val recorder = Recorder()
    private var isInitialized = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val MODEL_FILE_NAME = "models/ggml-base-q5_1.bin"
    }
    
    /**
     * 初始化Whisper模型
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                Log.d(TAG, "Whisper已经初始化")
                return@withContext true
            }
            
            // 优先使用直接从assets加载的方法（更高效）
            whisperContext = try {
                WhisperContext.createContextFromAsset(context.assets, MODEL_FILE_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "从assets直接加载模型失败: ${e.message}")
                // 如果直接加载失败，尝试从内部存储加载
                val modelFile = File(context.filesDir, MODEL_FILE_NAME)
                if (modelFile.exists()) {
                    WhisperContext.createContextFromFile(modelFile.absolutePath)
                } else {
                    Log.e(TAG, "模型文件不存在: $MODEL_FILE_NAME")
                    null
                }
            }
            
            isInitialized = whisperContext != null
            if (isInitialized) {
                Log.d(TAG, "Whisper初始化成功")
            } else {
                Log.e(TAG, "Whisper初始化失败")
            }
            
            return@withContext isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "初始化Whisper时发生错误", e)
            isInitialized = false
            return@withContext false
        }
    }
    
    /**
     * 开始录音
     */
    suspend fun startRecording(
        outputFile: File,
        onError: (Exception) -> Unit = {}
    ): Boolean {
        return try {
            recorder.startRecording(outputFile, onError)
            Log.d(TAG, "开始录音: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "开始录音失败", e)
            onError(e)
            false
        }
    }
    
    /**
     * 停止录音
     */
    suspend fun stopRecording() {
        try {
            recorder.stopRecording()
            Log.d(TAG, "录音已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
        }
    }
    
    /**
     * 将录音文件转换为文本
     */
    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Whisper未初始化")
            return@withContext "语音识别未初始化"
        }
        
        if (!audioFile.exists()) {
            Log.e(TAG, "音频文件不存在: ${audioFile.absolutePath}")
            return@withContext "音频文件不存在"
        }
        
        try {
            Log.d(TAG, "开始转录音频: ${audioFile.absolutePath}")
            
            // 将WAV文件转换为FloatArray
            val audioData = decodeWaveFile(audioFile)
            Log.d(TAG, "音频数据长度: ${audioData.size}")
            
            // 使用Whisper进行转录
            val result = whisperContext?.transcribeData(audioData, false) ?: "转录失败"
            
            Log.d(TAG, "转录结果: $result")
            return@withContext result.trim()
        } catch (e: Exception) {
            Log.e(TAG, "转录音频时发生错误", e)
            return@withContext "转录失败: ${e.message}"
        }
    }
    
    /**
     * 释放资源
     */
    suspend fun release() {
        try {
            whisperContext?.release()
            whisperContext = null
            isInitialized = false
            Log.d(TAG, "资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源时发生错误", e)
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 获取系统信息
     */
    suspend fun getSystemInfo(): String {
        return try {
            WhisperContext.getSystemInfo()
        } catch (e: Exception) {
            "获取系统信息失败: ${e.message}"
        }
    }
}
