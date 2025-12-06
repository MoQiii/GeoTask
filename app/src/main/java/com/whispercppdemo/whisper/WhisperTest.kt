package com.whispercppdemo.whisper

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.TaskControllerApi
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import org.openapitools.client.infrastructure.*
import org.openapitools.client.models.*
import timber.log.Timber
import java.io.IOException

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
        val whisperContext = WhisperLib.initContextFromAsset(context.assets, "models/ggml-small-q5_1.bin")
        if (whisperContext == 0L) {
            Log.e(TAG, "Failed to init Whisper context")
            return
        }

        // 2️⃣ 加载音频
        val audioData = loadWavFromAssets(context, "samples/samples_jfk.wav")
        Log.i(TAG, "Audio loaded, samples=${audioData.size}")

        // 3️⃣ 转录
        WhisperLib.fullTranscribe(whisperContext, audioData = audioData)

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
    // 挂起函数，异步 ping
    suspend fun pingIP(ip: String, timeoutMs: Int = 3000): Boolean = withContext(Dispatchers.IO) {
        try {
            // 使用 system ping
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W ${timeoutMs / 1000} $ip")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    suspend fun apitest(){
        val apiInstance = TaskControllerApi()
        val id : kotlin.Long = 4 // kotlin.Long |
        try {
            Timber.w("apitestapitest start")
            val ip = "10.20.0.1"
            val reachable = pingIP(ip)
            Timber.d("apitestapitest Ping $ip reachable = $reachable")
            val result : kotlin.Boolean = apiInstance.deleteTask(id)
            Timber.w("apitestapitest end")
            println(result)
        } catch (e: ClientException) {
            println("4xx response calling TaskControllerApi#deleteTask")
            e.printStackTrace()
        } catch (e: ServerException) {
            println("5xx response calling TaskControllerApi#deleteTask")
            e.printStackTrace()
        }
    }
}
