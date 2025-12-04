package com.whispercpp.whisper

import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Whisper 上下文类，封装了 Whisper 模型的操作
 */
class WhisperContext private constructor(private val contextPtr: Long) {
    private val TAG = "WhisperContext"
    private var isReleased = false

    companion object {
        /**
         * 从资产文件创建 Whisper 上下文
         */
        fun createContextFromAsset(assetManager: AssetManager, modelPath: String): WhisperContext? {
            return try {
                // 将模型文件从 assets 复制到临时文件
                val tempFile = File.createTempFile("whisper_model_", ".bin")
                tempFile.deleteOnExit()
                
                val inputStream = assetManager.open(modelPath)
                val outputStream = FileOutputStream(tempFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                createContextFromFile(tempFile.absolutePath)
            } catch (e: Exception) {
                Log.e("WhisperContext", "Failed to create context from asset: $modelPath", e)
                null
            }
        }

        /**
         * 从文件路径创建 Whisper 上下文
         */
        fun createContextFromFile(modelPath: String): WhisperContext? {
            return try {
                val contextPtr = WhisperLib.initContext(modelPath)
                if (contextPtr != 0L) {
                    WhisperContext(contextPtr)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("WhisperContext", "Failed to create context from file: $modelPath", e)
                null
            }
        }

        /**
         * 获取系统信息
         */
        fun getSystemInfo(): String {
            return try {
                // 这里可以调用 JNI 方法获取系统信息
                "Whisper System Info: Android ${android.os.Build.VERSION.RELEASE}"
            } catch (e: Exception) {
                "Failed to get system info: ${e.message}"
            }
        }
    }

    /**
     * 转录音频数据
     * @param audioData 音频数据（FloatArray）
     * @param translate 是否翻译（false 表示转录）
     * @return 转录结果文本
     */
    fun transcribeData(audioData: FloatArray, translate: Boolean = false): String {
        if (isReleased) {
            throw IllegalStateException("WhisperContext has been released")
        }

        return try {
            // 使用 WhisperLib 进行转录
            WhisperLib.fullTranscribe(contextPtr, 4, audioData)
            
            // 获取转录结果
            val segmentsCount = WhisperLib.getTextSegmentCount(contextPtr)
            val result = StringBuilder()
            
            for (i in 0 until segmentsCount) {
                val text = WhisperLib.getTextSegment(contextPtr, i)
                if (text.isNotEmpty()) {
                    if (result.isNotEmpty()) {
                        result.append(" ")
                    }
                    result.append(text)
                }
            }
            
            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            "Transcription failed: ${e.message}"
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        if (!isReleased) {
            try {
                WhisperLib.freeContext(contextPtr)
                isReleased = true
                Log.d(TAG, "WhisperContext released")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release WhisperContext", e)
            }
        }
    }

    /**
     * 检查是否已释放
     */
    fun isReleased(): Boolean = isReleased

    /**
     * 获取上下文指针（用于底层操作）
     */
    fun getContextPtr(): Long = contextPtr
}
