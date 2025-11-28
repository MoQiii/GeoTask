package com.syj.geotask.data.datasource.local

import androidx.room.*
import com.syj.geotask.domain.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTime ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted ORDER BY dueDate ASC, dueTime ASC")
    fun getTasksByCompletionStatus(isCompleted: Boolean): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' ORDER BY dueDate ASC, dueTime ASC")
    fun searchTasks(query: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletionStatus(id: Long, isCompleted: Boolean)
    
    @Query("UPDATE tasks SET isReminderEnabled = :isEnabled WHERE id = :id")
    suspend fun updateTaskReminderStatus(id: Long, isEnabled: Boolean)
}
