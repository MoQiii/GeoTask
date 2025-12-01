package com.syj.geotask.utils

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 自定义Timber树，将日志输出到文件
 */
class FileLoggingTree(private val context: Context) : Timber.Tree() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logDirectory = File(context.filesDir, "log")
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
            
            val fileName = "geotask_${fileNameFormat.format(Date())}.log"
            val logFile = File(logDirectory, fileName)
            
            val timestamp = dateFormat.format(Date())
            val priorityStr = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
            
            val logEntry = StringBuilder()
            logEntry.append("$timestamp ")
            logEntry.append("$priorityStr/")
            logEntry.append("$tag: ")
            logEntry.append(message)
            
            if (t != null) {
                logEntry.append("\n")
                logEntry.append(Log.getStackTraceString(t))
            }
            logEntry.append("\n")
            
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry.toString())
                writer.flush()
            }
            
            // 限制日志文件数量，保留最近7天的日志
            limitLogFiles(logDirectory, 7)
            
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "写入日志文件失败", e)
        }
    }
    
    /**
     * 限制日志文件数量，只保留指定天数的日志
     */
    private fun limitLogFiles(logDirectory: File, daysToKeep: Int) {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
            val cutoffDate = calendar.time
            
            logDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("geotask_") && file.name.endsWith(".log")
            }?.forEach { file ->
                val fileDateStr = file.name.substringAfter("geotask_").substringBefore(".log")
                val fileDate = fileNameFormat.parse(fileDateStr)
                
                if (fileDate != null && fileDate.before(cutoffDate)) {
                    if (file.delete()) {
                        Log.d("FileLoggingTree", "删除过期日志文件: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "清理日志文件失败", e)
        }
    }
    
    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): Array<File> {
        val logDirectory = File(context.filesDir, "log")
        return if (logDirectory.exists()) {
            logDirectory.listFiles { file ->
                file.isFile && file.name.startsWith("geotask_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }?.toTypedArray() ?: emptyArray()
        } else {
            emptyArray()
        }
    }
    
    /**
     * 清空所有日志文件
     */
    fun clearAllLogs() {
        try {
            val logDirectory = File(context.filesDir, "log")
            if (logDirectory.exists()) {
                logDirectory.listFiles { file ->
                    file.isFile && file.name.startsWith("geotask_") && file.name.endsWith(".log")
                }?.forEach { file ->
                    if (file.delete()) {
                        Log.d("FileLoggingTree", "删除日志文件: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "清空日志文件失败", e)
        }
    }
}
