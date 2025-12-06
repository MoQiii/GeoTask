package com.syj.geotask.di

import android.content.Context
import androidx.room.Room
import com.syj.geotask.data.datasource.local.AppDatabase
import com.syj.geotask.data.datasource.local.TaskDao
import com.syj.geotask.data.datasource.local.UserDao
import com.syj.geotask.data.repository.TaskRepositoryImpl
import com.syj.geotask.data.repository.UserRepositoryImpl
import com.syj.geotask.data.service.GeofenceManagerFactory
import com.syj.geotask.data.service.IGeofenceManager
import com.syj.geotask.data.service.NotificationService
import com.syj.geotask.data.service.TaskReminderManager
import com.syj.geotask.domain.repository.TaskRepository
import com.syj.geotask.domain.repository.UserRepository
import com.syj.geotask.domain.usecase.*
import com.syj.geotask.presentation.theme.ThemeManager
import com.syj.geotask.speech.SpeechToTextManager
import com.syj.geotask.speech.VoiceTaskManager
import org.openapitools.client.apis.TaskControllerApi
import org.openapitools.client.apis.AiControllerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
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
    fun provideTaskControllerApi(): TaskControllerApi {
        return TaskControllerApi()
    }

    @Provides
    @Singleton
    fun provideAiControllerApi(): AiControllerApi {
        // 创建带有15秒超时设置的OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        
        return AiControllerApi(client = okHttpClient)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskControllerApi: TaskControllerApi,
        geofenceManager: IGeofenceManager,
        @ApplicationContext context: Context
    ): TaskRepository {
        return TaskRepositoryImpl(taskControllerApi, geofenceManager, context)
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
    fun provideGeofenceManagerFactory(@ApplicationContext context: Context): GeofenceManagerFactory {
        return GeofenceManagerFactory(context)
    }

    @Provides
    @Singleton
    fun provideGeofenceManager(geofenceManagerFactory: GeofenceManagerFactory): IGeofenceManager {
        return geofenceManagerFactory.getGeofenceManager()
    }

    @Provides
    @Singleton
    fun provideThemeManager(@ApplicationContext context: Context): ThemeManager {
        return ThemeManager(context)
    }

    // Task related UseCases
    @Provides
    @Singleton
    fun provideGetTasksUseCase(taskRepository: TaskRepository): GetTasksUseCase {
        return GetTasksUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideAddTaskUseCase(taskRepository: TaskRepository): AddTaskUseCase {
        return AddTaskUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideAddTaskWithGeofenceUseCase(taskRepository: TaskRepository): AddTaskWithGeofenceUseCase {
        return AddTaskWithGeofenceUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateTaskUseCase(taskRepository: TaskRepository): UpdateTaskUseCase {
        return UpdateTaskUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateTaskWithGeofenceUseCase(taskRepository: TaskRepository): UpdateTaskWithGeofenceUseCase {
        return UpdateTaskWithGeofenceUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteTaskUseCase(taskRepository: TaskRepository): DeleteTaskUseCase {
        return DeleteTaskUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteTaskWithGeofenceUseCase(taskRepository: TaskRepository): DeleteTaskWithGeofenceUseCase {
        return DeleteTaskWithGeofenceUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideTaskReminderManager(@ApplicationContext context: Context): TaskReminderManager {
        return TaskReminderManager(context)
    }

    @Provides
    @Singleton
    fun provideSpeechToTextManager(@ApplicationContext context: Context): SpeechToTextManager {
        return SpeechToTextManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceTaskManager(
        @ApplicationContext context: Context,
        speechToTextManager: SpeechToTextManager,
        aiControllerApi: AiControllerApi
    ): VoiceTaskManager {
        return VoiceTaskManager(context, speechToTextManager, aiControllerApi)
    }
}
