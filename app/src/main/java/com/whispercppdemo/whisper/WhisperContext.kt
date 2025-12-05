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
         * 从资产文件创建 Whisper 上下文（推荐方法，直接从assets加载）
         */
        fun createContextFromAsset(assetManager: AssetManager, modelPath: String): WhisperContext? {
            return try {
                val contextPtr = WhisperLib.initContextFromAsset(assetManager, modelPath)
                if (contextPtr != 0L) {
                    WhisperContext(contextPtr)
                } else {
                    Log.w("WhisperContext", "Direct asset loading failed, trying legacy method")
                    // 如果直接加载失败，尝试传统方法
                    createContextFromAssetLegacy(assetManager, modelPath)
                }
            } catch (e: Exception) {
                Log.e("WhisperContext", "Failed to create context from asset: $modelPath", e)
                // 尝试传统方法作为后备
                createContextFromAssetLegacy(assetManager, modelPath)
            }
        }

        /**
         * 从资产文件创建 Whisper 上下文（传统方法，复制到临时文件）
         */
        private fun createContextFromAssetLegacy(assetManager: AssetManager, modelPath: String): WhisperContext? {
            return try {
                val contextPtr = WhisperLib.initContextFromAssetLegacy(assetManager, modelPath)
                if (contextPtr != 0L) {
                    WhisperContext(contextPtr)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("WhisperContext", "Failed to create context from asset (legacy): $modelPath", e)
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
         * 获取 Whisper 系统信息
         * @return 包含 CPU 信息、编译选项、支持的指令集等详细信息的字符串
         */
        fun getSystemInfo(): String {
            return try {
                WhisperLib.getSystemInfo()
            } catch (e: Exception) {
                "Failed to get system info: ${e.message}"
            }
        }
    }

    /**
     * 转录音频数据
     * 
     * ⚠️ 重要警告：此方法使用的是 fullTranscribe JNI 函数，该函数在 C++ 层硬编码语言为 "en"（英语），
     * 因此无法正确识别中文语音。如果您需要识别中文，请参考以下解决方案：
     * 
     * 解决方案：
     * 1. 修改 JNI 层的 fullTranscribe 函数，将 params.language 从 "en" 改为 "zh"
     * 2. 或者添加一个新的带语言参数的 JNI 函数 fullTranscribeWithLanguage
     * 3. 重新编译 JNI 库
     * 
     * @param audioData 音频数据（16kHz 单声道 FloatArray）
     * @param translate 是否翻译（false 表示转录，true 表示翻译为英语）
     * @return 转录结果文本（中文语音会被错误识别为英语）
     */
    fun transcribeData(audioData: FloatArray, translate: Boolean = false): String {
        if (isReleased) {
            throw IllegalStateException("WhisperContext has been released")
        }

        return try {
            // 使用 WhisperLib 进行转录
            // 注意：fullTranscribe 在 JNI 层硬编码语言为 "en"，这是中文识别问题的根源
            WhisperLib.fullTranscribe(contextPtr=contextPtr, audioData=audioData)
            
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
