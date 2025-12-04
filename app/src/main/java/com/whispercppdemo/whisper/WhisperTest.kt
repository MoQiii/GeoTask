package com.whispercppdemo.whisper

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperLib
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WhisperTest {
    private const val TAG = "WhisperTest"

    /**
     * 从 asset 加载 WAV 并转成 FloatArray PCM
     */
    private fun loadWavFromAssets(context: Context, assetPath: String): FloatArray {
        val inputStream: InputStream = context.assets.open(assetPath)
        val header = ByteArray(44)
        inputStream.read(header, 0, 44) // 跳过 WAV 头

        val bytes = inputStream.readBytes()
        val floatArray = FloatArray(bytes.size / 2)

        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in floatArray.indices) {
            floatArray[i] = bb.short.toFloat() / Short.MAX_VALUE
        }
        inputStream.close()
        return floatArray
    }

    /**
     * 测试转录方法
     */
    fun testTranscribe(context: Context) {
        // 1️⃣ 初始化 Whisper
        val modelPath = "samples/samples_jfk.wav" // 只是示例，你可以换成模型 bin 文件
        // 从 Asset 初始化模型
        val whisperContext = WhisperLib.initContextFromAsset(context.assets, "models/ggml-small.en-q5_1.bin")
        if (whisperContext == 0L) {
            Log.e(TAG, "Failed to init Whisper context")
            return
        }

        // 2️⃣ 加载音频
        val audioData = loadWavFromAssets(context, "samples/samples_jfk.wav")
        Log.i(TAG, "Audio loaded, samples=${audioData.size}")

        // 3️⃣ 转录
        WhisperLib.fullTranscribe(whisperContext, numThreads = 4, audioData = audioData)

        // 4️⃣ 获取结果
        val segmentsCount = WhisperLib.getTextSegmentCount(whisperContext)
        Log.i(TAG, "Transcription segments count: $segmentsCount")

        for (i in 0 until segmentsCount) {
            val text = WhisperLib.getTextSegment(whisperContext, i)
            val t0 = WhisperLib.getTextSegmentT0(whisperContext, i)
            val t1 = WhisperLib.getTextSegmentT1(whisperContext, i)
            Log.i(TAG, "[$t0 ms - $t1 ms]: $text")
        }

        // 5️⃣ 释放
        WhisperLib.freeContext(whisperContext)
    }
}
