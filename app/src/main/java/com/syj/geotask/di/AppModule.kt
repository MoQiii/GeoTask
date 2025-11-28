package com.syj.geotask.di

import android.content.Context
import androidx.room.Room
import com.syj.geotask.data.datasource.local.AppDatabase
import com.syj.geotask.data.datasource.local.TaskDao
import com.syj.geotask.data.datasource.local.UserDao
import com.syj.geotask.data.repository.TaskRepositoryImpl
import com.syj.geotask.data.repository.UserRepositoryImpl
import com.syj.geotask.data.service.GeofenceManager
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.domain.repository.TaskRepository
import com.syj.geotask.domain.repository.UserRepository
import com.syj.geotask.domain.usecase.LoginUseCase
import com.syj.geotask.domain.usecase.RegisterUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module  //表示下面存放依赖注入的配方
@InstallIn(SingletonComponent::class)
//| Component          | 生命周期           | 适合提供的依赖                   |
//| ------------------ | -------------- | ------------------------- |
//| SingletonComponent | Application 全局 | Repository、Retrofit、数据库   |
//| ActivityComponent  | Activity 生命周期  | Activity 相关依赖             |
//| FragmentComponent  | Fragment 生命周期  | Fragment 相关依赖             |
//| ViewModelComponent | ViewModel 生命周期 | UseCase、ViewModel helpers |
//| ViewComponent      | View 生命周期      | 纯 UI 依赖                   |
object AppModule {

    @Provides
    @Singleton
    //实例的构造方式  @ApplicationContext 限定符 Application的context
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "geotask_database"
        ).build()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepositoryImpl(userDao)
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(userRepository: UserRepository): LoginUseCase {
        return LoginUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideRegisterUseCase(userRepository: UserRepository): RegisterUseCase {
        return RegisterUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext context: Context): NotificationService {
        return NotificationService(context)
    }

    @Provides
    @Singleton
    fun provideGeofenceManager(@ApplicationContext context: Context): GeofenceManager {
        return GeofenceManager(context)
    }
}
