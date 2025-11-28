package com.syj.geotask.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long, // 时间戳
    val dueTime: Long, // 时间戳
    val isCompleted: Boolean = false,
    val isReminderEnabled: Boolean = false,
    val location: String? = null, // 地址描述
    val latitude: Double? = null, // 纬度
    val longitude: Double? = null, // 经度
    val geofenceRadius: Float = 200f, // 地理围栏半径（米）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
