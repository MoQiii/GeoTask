package com.whispercpp.whisper

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

class WhisperLib {

    companion object {
        init {
            System.loadLibrary("whisper_jni") // 对应 CMake 生成的 so 文件名
        }

        /**
         * 对应 JNI 函数:
         * Java_com_whispercpp_whisper_WhisperLib_00024Companion_initContext
         */
        external fun initContext(modelPath: String): Long

        /**
         * 对应 JNI 函数:
         * Java_com_whispercpp_whisper_WhisperLib_00024Companion_freeContext
         */
        external fun freeContext(contextPtr: Long)

        /**
         * 从资产文件初始化上下文
         */
        fun initContextFromAsset(assetManager: AssetManager, modelPath: String): Long {
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

        /**
         * 完整转录音频数据
         * 对应 JNI 函数
         */
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)

        /**
         * 获取文本段数量
         * 对应 JNI 函数
         */
        external fun getTextSegmentCount(contextPtr: Long): Int

        /**
         * 获取指定索引的文本段
         * 对应 JNI 函数
         */
        external fun getTextSegment(contextPtr: Long, index: Int): String

        /**
         * 获取文本段的开始时间
         * 对应 JNI 函数
         */
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long

        /**
         * 获取文本段的结束时间
         * 对应 JNI 函数
         */
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long
    }
}
