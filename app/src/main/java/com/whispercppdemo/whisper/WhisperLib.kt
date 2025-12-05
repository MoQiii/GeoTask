package com.whispercpp.whisper

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

/**
 * Whisper JNI 库的 Kotlin 封装类
 * 提供了与底层 C++ Whisper 库的接口
 */
class WhisperLib {

    data class TranscribeParams(
        val printRealtime: Boolean=true,
        val printProgress: Boolean=false,
        val printTimestamps: Boolean=true,
        val printSpecial: Boolean=false,
        val translate: Boolean=false,
        val language: String="zh",
        val nThreads: Int=4,
        val offsetMs: Int=0,
        val noContext: Boolean=true,
        val singleSegment: Boolean=false
    )

    companion object {
        init {
            System.loadLibrary("whisper_jni") // 对应 CMake 生成的 so 文件名
        }

        // ==================== 上下文初始化相关方法 ====================

        /**
         * 从文件路径初始化 Whisper 上下文
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContext
         * 
         * @param modelPath 模型文件的完整路径
         * @return Whisper 上下文指针，如果失败返回 0L
         */
        external fun initContext(modelPath: String): Long

        /**
         * 从 AssetManager 直接初始化 Whisper 上下文（无需复制到临时文件）
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContextFromAsset
         * 
         * @param assetManager Android AssetManager 实例
         * @param assetPath assets 目录中的模型文件路径（如 "models/ggml-small-q5_1.bin"）
         * @return Whisper 上下文指针，如果失败返回 0L
         */
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long

        /**
         * 从输入流初始化 Whisper 上下文
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContextFromInputStream
         * 
         * @param inputStream 模型文件的输入流
         * @return Whisper 上下文指针，如果失败返回 0L
         */
        external fun initContextFromInputStream(inputStream: java.io.InputStream): Long

        /**
         * 释放 Whisper 上下文资源
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_freeContext
         * 
         * @param contextPtr Whisper 上下文指针
         */
        external fun freeContext(contextPtr: Long)

        // ==================== 音频转录相关方法 ====================

        /**
         * 完整转录音频数据（硬编码为英语识别）
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_fullTranscribe
         *
         * 
         * @param contextPtr Whisper 上下文指针
         * @param numThreads 使用的线程数量，通常为 4
         * @param audioData 音频数据数组（16kHz 单声道 FloatArray）
         */
//        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)
        external fun fullTranscribe(
            contextPtr: Long,
            params: TranscribeParams=TranscribeParams(),
            audioData: FloatArray
        )

        // ==================== 转录结果获取方法 ====================

        /**
         * 获取转录结果的文本段数量
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentCount
         * 
         * @param contextPtr Whisper 上下文指针
         * @return 文本段数量
         */
        external fun getTextSegmentCount(contextPtr: Long): Int

        /**
         * 获取指定索引的文本段内容
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegment
         * 
         * @param contextPtr Whisper 上下文指针
         * @param index 文本段索引（从 0 开始）
         * @return 文本段内容字符串
         */
        external fun getTextSegment(contextPtr: Long, index: Int): String

        /**
         * 获取文本段的开始时间（毫秒）
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentT0
         * 
         * @param contextPtr Whisper 上下文指针
         * @param index 文本段索引（从 0 开始）
         * @return 开始时间戳（毫秒）
         */
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long

        /**
         * 获取文本段的结束时间（毫秒）
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_getTextSegmentT1
         * 
         * @param contextPtr Whisper 上下文指针
         * @param index 文本段索引（从 0 开始）
         * @return 结束时间戳（毫秒）
         */
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long

        // ==================== 系统信息和性能测试方法 ====================

        /**
         * 获取 Whisper 系统信息
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_getSystemInfo
         * 
         * @return 包含系统信息的字符串（如 CPU 信息、编译选项等）
         */
        external fun getSystemInfo(): String

        /**
         * 执行内存复制性能基准测试
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_benchMemcpy
         * 
         * @param nThreads 测试使用的线程数量
         * @return 内存复制性能测试结果字符串
         */
        external fun benchMemcpy(nThreads: Int): String

        /**
         * 执行矩阵乘法性能基准测试
         * 对应 JNI 函数: Java_com_whispercpp_whisper_WhisperLib_00024Companion_benchGgmlMulMat
         * 
         * @param nThreads 测试使用的线程数量
         * @return 矩阵乘法性能测试结果字符串
         */
        external fun benchGgmlMulMat(nThreads: Int): String

        // ==================== 便利方法 ====================

        /**
         * 从资产文件初始化上下文（兼容性方法，复制到临时文件）
         * 
         * @param assetManager Android AssetManager 实例
         * @param modelPath assets 目录中的模型文件路径
         * @return Whisper 上下文指针，如果失败返回 0L
         */
        fun initContextFromAssetLegacy(assetManager: AssetManager, modelPath: String): Long {
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
                
                initContext(tempFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }
}
