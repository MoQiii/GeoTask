package com.syj.geotask.speech

import android.content.Context
import com.syj.geotask.media.decodeWaveFile
import com.syj.geotask.recorder.Recorder
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class SpeechToTextManager(private val context: Context) {
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
                Timber.d("Whisper已经初始化")
                return@withContext true
            }
            
            Timber.d("开始初始化Whisper模型...")
            Timber.d("模型文件路径: $MODEL_FILE_NAME")
            
            // 检查assets中是否存在模型文件
            try {
                val assetList = context.assets.list("")
                val modelsList = context.assets.list("models")
                Timber.d("Assets根目录文件: ${assetList?.joinToString(", ")}")
                Timber.d("Models目录文件: ${modelsList?.joinToString(", ")}")
                
                val modelExists = context.assets.list("models")?.contains("ggml-base-q5_1.bin") == true
                Timber.d("模型文件是否存在: $modelExists")
            } catch (e: Exception) {
                Timber.e(e, "检查assets文件失败")
            }
            
            // 优先使用直接从assets加载的方法（更高效）
            whisperContext = try {
                Timber.d("尝试从assets直接加载模型...")
                WhisperContext.createContextFromAsset(context.assets, MODEL_FILE_NAME)
            } catch (e: Exception) {
                Timber.w("从assets直接加载模型失败: ${e.message}")
                Timber.w("异常类型: ${e.javaClass.simpleName}")
                
                // 如果直接加载失败，尝试从内部存储加载
                val modelFile = File(context.filesDir, MODEL_FILE_NAME)
                Timber.d("尝试从内部存储加载模型: ${modelFile.absolutePath}")
                Timber.d("文件是否存在: ${modelFile.exists()}")
                
                if (modelFile.exists()) {
                    try {
                        WhisperContext.createContextFromFile(modelFile.absolutePath)
                    } catch (e2: Exception) {
                        Timber.e("从内部存储加载模型也失败: ${e2.message}")
                        null
                    }
                } else {
                    Timber.e("内部存储中模型文件不存在: $MODEL_FILE_NAME")
                    null
                }
            }
            
            isInitialized = whisperContext != null
            if (isInitialized) {
                Timber.d("✅ Whisper初始化成功")
                
                // 测试系统信息
                try {
                    val systemInfo = WhisperContext.getSystemInfo()
                    Timber.d("Whisper系统信息: $systemInfo")
                } catch (e: Exception) {
                    Timber.w("获取Whisper系统信息失败: ${e.message}")
                }
            } else {
                Timber.e("❌ Whisper初始化失败")
                
                // 尝试获取更多调试信息
                try {
                    val systemInfo = WhisperContext.getSystemInfo()
                    Timber.d("Whisper库可用，系统信息: $systemInfo")
                } catch (e: Exception) {
                    Timber.e("Whisper库不可用: ${e.message}")
                }
            }
            
            return@withContext isInitialized
        } catch (e: Exception) {
            Timber.e(e, "初始化Whisper时发生错误")
            Timber.e("异常堆栈: ${e.stackTraceToString()}")
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
        Timber.d("🎙️ SpeechToTextManager.startRecording() 被调用")
        Timber.d("📁 输出文件: ${outputFile.absolutePath}")
        return try {
            Timber.d("🎤 调用 recorder.startRecording()...")
            val success = recorder.startRecording(outputFile, onError)
            Timber.d("🎤 recorder.startRecording() 返回: $success")
            if (success) {
                Timber.d("✅ 开始录音成功: ${outputFile.absolutePath}")
            } else {
                Timber.e("❌ recorder.startRecording() 返回 false")
            }
            success
        } catch (e: Exception) {
            Timber.e(e, "❌ 开始录音时发生异常")
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
            Timber.d("录音已停止")
        } catch (e: Exception) {
            Timber.e(e, "停止录音失败")
        }
    }
    
    /**
     * 将录音文件转换为文本
     */
    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Timber.e("Whisper未初始化")
            return@withContext "语音识别未初始化"
        }
        
        if (!audioFile.exists()) {
            Timber.e("音频文件不存在: ${audioFile.absolutePath}")
            return@withContext "音频文件不存在"
        }
        
        try {
            Timber.d("开始转录音频: ${audioFile.absolutePath}")
            
            // 将WAV文件转换为FloatArray
            val audioData = decodeWaveFile(audioFile)
            Timber.d("音频数据长度: ${audioData.size}")
            
            // 使用Whisper进行转录
            val result = whisperContext?.transcribeData(audioData, false) ?: "转录失败"
            
            Timber.d("转录结果: $result")
            return@withContext result.trim()
        } catch (e: Exception) {
            Timber.e(e, "转录音频时发生错误")
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
            Timber.d("资源已释放")
        } catch (e: Exception) {
            Timber.e(e, "释放资源时发生错误")
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
