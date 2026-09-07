package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeficitCacheDao {
    @Query("SELECT * FROM deficit_cache WHERE id = 1 LIMIT 1")
    fun getLatestDeficitAnalysis(): Flow<DeficitCacheEntity?>

    @Query("SELECT * FROM deficit_cache WHERE id = 1 LIMIT 1")
    suspend fun getLatestDeficitAnalysisSync(): DeficitCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDeficitAnalysis(entity: DeficitCacheEntity)
}
