package com.syj.geotask.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.syj.geotask.domain.model.Task
import com.syj.geotask.domain.model.User

@Database(entities = [Task::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
}
