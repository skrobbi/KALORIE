package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getMealsSince(startTime: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getMealsSinceSync(startTime: Long): List<MealEntity>

    @Query("SELECT * FROM meals WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMealsBetween(startTime: Long, endTime: Long): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("SELECT COUNT(*) FROM meals")
    fun getMealCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM meals WHERE fromCache = 1")
    fun getCachedMealsCount(): Flow<Int>
}
