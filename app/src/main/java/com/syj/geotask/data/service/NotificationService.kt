package com.syj.geotask.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.syj.geotask.MainActivity
import com.syj.geotask.R

class NotificationService(private val context: Context) {

    companion object {
        const val TASK_REMINDER_CHANNEL_ID = "task_reminder_channel"
        const val TASK_REMINDER_CHANNEL_NAME = "任务提醒"
        const val TASK_REMINDER_CHANNEL_DESCRIPTION = "任务到期提醒通知"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TASK_REMINDER_CHANNEL_ID,
                TASK_REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = TASK_REMINDER_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminderNotification(
        taskId: Long,
        taskTitle: String,
        taskDescription: String,
        taskTime: String
    ) {
        // 创建点击通知后的Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", taskId)
            putExtra("navigate_to", "task_detail")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("任务提醒")
            .setContentText("$taskTime $taskTitle")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$taskTime $taskTitle\n$taskDescription")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(taskId.toInt(), notification)
        }
    }

    fun showLocationReminderNotification(
        taskId: Long,
        taskTitle: String,
        location: String
    ) {
        // 创建点击通知后的Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", taskId)
            putExtra("navigate_to", "task_detail")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TASK_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("位置提醒")
            .setContentText("您已到达 $location")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("您已到达 $location\n任务: $taskTitle")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(taskId.toInt() + 10000, notification) // 使用不同的ID避免冲突
        }
    }

    fun cancelNotification(taskId: Long) {
        with(NotificationManagerCompat.from(context)) {
            cancel(taskId.toInt())
            cancel(taskId.toInt() + 10000) // 也取消位置提醒通知
        }
    }
}
