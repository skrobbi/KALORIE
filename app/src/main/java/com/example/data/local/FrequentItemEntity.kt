package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FoodItem
import com.example.data.model.Micronutrients

@Entity(tableName = "frequent_items")
data class FrequentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedName: String, // lowercase trimmed query, e.g. "яблоко", "кофе черный"
    val name: String,
    val estimatedWeightG: Double,
    val calories: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val vitaminDMcg: Double = 0.0,
    val ironMg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val omega3G: Double = 0.0,
    val zincMg: Double = 0.0,
    val iodineMcg: Double = 0.0,
    val timesUsed: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
) {
    fun toFoodItem(): FoodItem {
        return FoodItem(
            name = name,
            estimatedWeightG = estimatedWeightG,
            calories = calories,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            micronutrients = Micronutrients(
                vitaminDMcg = vitaminDMcg,
                ironMg = ironMg,
                magnesiumMg = magnesiumMg,
                calciumMg = calciumMg,
                vitaminB12Mcg = vitaminB12Mcg,
                omega3G = omega3G,
                zincMg = zincMg,
                iodineMcg = iodineMcg
            )
        )
    }

    companion object {
        fun fromFoodItem(item: FoodItem, queryKey: String? = null): FrequentItemEntity {
            val key = (queryKey ?: item.name).trim().lowercase()
            return FrequentItemEntity(
                normalizedName = key,
                name = item.name,
                estimatedWeightG = item.estimatedWeightG,
                calories = item.calories,
                proteinG = item.proteinG,
                fatG = item.fatG,
                carbsG = item.carbsG,
                vitaminDMcg = item.micronutrients.vitaminDMcg,
                ironMg = item.micronutrients.ironMg,
                magnesiumMg = item.micronutrients.magnesiumMg,
                calciumMg = item.micronutrients.calciumMg,
                vitaminB12Mcg = item.micronutrients.vitaminB12Mcg,
                omega3G = item.micronutrients.omega3G,
                zincMg = item.micronutrients.zincMg,
                iodineMcg = item.micronutrients.iodineMcg
            )
        }
    }
}
