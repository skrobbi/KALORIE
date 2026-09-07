package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequentItemDao {
    @Query("SELECT * FROM frequent_items WHERE normalizedName = :name LIMIT 1")
    suspend fun findByName(name: String): FrequentItemEntity?

    @Query("SELECT * FROM frequent_items WHERE normalizedName LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY timesUsed DESC LIMIT 1")
    suspend fun searchFirst(query: String): FrequentItemEntity?

    @Query("SELECT * FROM frequent_items ORDER BY timesUsed DESC, lastUsed DESC")
    fun getAllFrequentItems(): Flow<List<FrequentItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FrequentItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<FrequentItemEntity>)

    @Query("UPDATE frequent_items SET timesUsed = timesUsed + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM frequent_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM frequent_items")
    fun getItemsCount(): Flow<Int>
}
