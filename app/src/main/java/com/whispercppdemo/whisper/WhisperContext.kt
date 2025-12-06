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
     * ✅ 修复说明：此方法现在使用正确的中文语言参数进行转录
     * 
     * @param audioData 音频数据（16kHz 单声道 FloatArray）
     * @param translate 是否翻译（false 表示转录，true 表示翻译为英语）
     * @return 转录结果文本（现在可以正确识别中文语音）
     */
    fun transcribeData(audioData: FloatArray, translate: Boolean = false): String {
        if (isReleased) {
            throw IllegalStateException("WhisperContext has been released")
        }

        return try {
            // 使用 WhisperLib 进行转录，设置正确的中文语言参数
            val params = WhisperLib.TranscribeParams(
                printRealtime = false,
                printProgress = false,
                printTimestamps = false,
                printSpecial = false,
                translate = translate,
                language = "zh", // 设置为中文
                nThreads = 4,
                offsetMs = 0,
                noContext = true,
                singleSegment = false
            )
            
            Log.d(TAG, "开始转录，语言设置: ${params.language}")
            WhisperLib.fullTranscribe(contextPtr = contextPtr, params = params, audioData = audioData)
            
            // 获取转录结果
            val segmentsCount = WhisperLib.getTextSegmentCount(contextPtr)
            Log.d(TAG, "转录段数量: $segmentsCount")
            val result = StringBuilder()
            
            for (i in 0 until segmentsCount) {
                val text = WhisperLib.getTextSegment(contextPtr, i)
                Log.d(TAG, "转录段 $i: $text")
                if (text.isNotEmpty()) {
                    if (result.isNotEmpty()) {
                        result.append(" ")
                    }
                    result.append(text)
                }
            }
            
            val finalResult = result.toString()
            Log.d(TAG, "最终转录结果: $finalResult")
            finalResult
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
