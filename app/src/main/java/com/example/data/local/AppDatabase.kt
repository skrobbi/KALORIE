package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MealEntity::class, FrequentItemEntity::class, DeficitCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun frequentItemDao(): FrequentItemDao
    abstract fun deficitCacheDao(): DeficitCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutritrack_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate with essential starter items for instant 0-token lookup
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialFrequentItems(database.frequentItemDao())
                    }
                }
            }
        }

        private suspend fun populateInitialFrequentItems(dao: FrequentItemDao) {
            val starterItems = listOf(
                FrequentItemEntity(
                    normalizedName = "яблоко",
                    name = "Яблоко зеленое",
                    estimatedWeightG = 150.0,
                    calories = 78.0,
                    proteinG = 0.4,
                    fatG = 0.2,
                    carbsG = 20.7,
                    vitaminDMcg = 0.0,
                    ironMg = 0.2,
                    magnesiumMg = 7.5,
                    calciumMg = 9.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.01,
                    zincMg = 0.1,
                    iodineMcg = 1.8,
                    timesUsed = 3
                ),
                FrequentItemEntity(
                    normalizedName = "яблоко зеленое",
                    name = "Яблоко зеленое 1 шт",
                    estimatedWeightG = 150.0,
                    calories = 78.0,
                    proteinG = 0.4,
                    fatG = 0.2,
                    carbsG = 20.7,
                    vitaminDMcg = 0.0,
                    ironMg = 0.2,
                    magnesiumMg = 7.5,
                    calciumMg = 9.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.01,
                    zincMg = 0.1,
                    iodineMcg = 1.8,
                    timesUsed = 5
                ),
                FrequentItemEntity(
                    normalizedName = "кофе",
                    name = "Кофе черный без сахара",
                    estimatedWeightG = 200.0,
                    calories = 2.0,
                    proteinG = 0.2,
                    fatG = 0.0,
                    carbsG = 0.0,
                    vitaminDMcg = 0.0,
                    ironMg = 0.02,
                    magnesiumMg = 6.0,
                    calciumMg = 4.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.0,
                    zincMg = 0.04,
                    iodineMcg = 0.0,
                    timesUsed = 6
                ),
                FrequentItemEntity(
                    normalizedName = "кофе черный",
                    name = "Кофе черный",
                    estimatedWeightG = 200.0,
                    calories = 2.0,
                    proteinG = 0.2,
                    fatG = 0.0,
                    carbsG = 0.0,
                    vitaminDMcg = 0.0,
                    ironMg = 0.02,
                    magnesiumMg = 6.0,
                    calciumMg = 4.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.0,
                    zincMg = 0.04,
                    iodineMcg = 0.0,
                    timesUsed = 4
                ),
                FrequentItemEntity(
                    normalizedName = "банан",
                    name = "Банан спелый",
                    estimatedWeightG = 120.0,
                    calories = 105.0,
                    proteinG = 1.3,
                    fatG = 0.3,
                    carbsG = 27.0,
                    vitaminDMcg = 0.0,
                    ironMg = 0.3,
                    magnesiumMg = 32.0,
                    calciumMg = 6.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.03,
                    zincMg = 0.2,
                    iodineMcg = 3.0,
                    timesUsed = 4
                ),
                FrequentItemEntity(
                    normalizedName = "яйцо вареное",
                    name = "Яйцо куриное вареное",
                    estimatedWeightG = 55.0,
                    calories = 78.0,
                    proteinG = 6.3,
                    fatG = 5.3,
                    carbsG = 0.6,
                    vitaminDMcg = 1.1,
                    ironMg = 0.9,
                    magnesiumMg = 6.0,
                    calciumMg = 28.0,
                    vitaminB12Mcg = 0.6,
                    omega3G = 0.05,
                    zincMg = 0.6,
                    iodineMcg = 25.0,
                    timesUsed = 8
                ),
                FrequentItemEntity(
                    normalizedName = "овсянка",
                    name = "Овсяная каша на воде",
                    estimatedWeightG = 200.0,
                    calories = 140.0,
                    proteinG = 5.0,
                    fatG = 3.0,
                    carbsG = 24.0,
                    vitaminDMcg = 0.0,
                    ironMg = 1.8,
                    magnesiumMg = 55.0,
                    calciumMg = 20.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 0.08,
                    zincMg = 1.5,
                    iodineMcg = 4.0,
                    timesUsed = 5
                ),
                FrequentItemEntity(
                    normalizedName = "гречка с курицей",
                    name = "Гречка с куриным филе",
                    estimatedWeightG = 250.0,
                    calories = 320.0,
                    proteinG = 32.0,
                    fatG = 4.5,
                    carbsG = 38.0,
                    vitaminDMcg = 0.1,
                    ironMg = 3.4,
                    magnesiumMg = 110.0,
                    calciumMg = 25.0,
                    vitaminB12Mcg = 0.4,
                    omega3G = 0.06,
                    zincMg = 2.4,
                    iodineMcg = 7.0,
                    timesUsed = 7
                ),
                FrequentItemEntity(
                    normalizedName = "творог 5%",
                    name = "Творог 5%",
                    estimatedWeightG = 150.0,
                    calories = 180.0,
                    proteinG = 25.5,
                    fatG = 7.5,
                    carbsG = 4.5,
                    vitaminDMcg = 0.15,
                    ironMg = 0.5,
                    magnesiumMg = 34.0,
                    calciumMg = 240.0,
                    vitaminB12Mcg = 0.7,
                    omega3G = 0.02,
                    zincMg = 1.2,
                    iodineMcg = 12.0,
                    timesUsed = 4
                ),
                FrequentItemEntity(
                    normalizedName = "грецкие орехи",
                    name = "Грецкие орехи (горсть)",
                    estimatedWeightG = 30.0,
                    calories = 196.0,
                    proteinG = 4.5,
                    fatG = 19.5,
                    carbsG = 4.1,
                    vitaminDMcg = 0.0,
                    ironMg = 0.9,
                    magnesiumMg = 47.0,
                    calciumMg = 29.0,
                    vitaminB12Mcg = 0.0,
                    omega3G = 2.7,
                    zincMg = 0.9,
                    iodineMcg = 1.0,
                    timesUsed = 2
                )
            )
            dao.insertAll(starterItems)
        }
    }
}
