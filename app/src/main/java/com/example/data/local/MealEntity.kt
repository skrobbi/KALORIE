package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FoodItem
import com.example.data.model.Micronutrients

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: String = "Прием пищи",
    val title: String,
    val itemsJson: String = "",
    val totalCalories: Double,
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
    val fromCache: Boolean = false
) {
    fun toMicronutrients(): Micronutrients {
        return Micronutrients(
            vitaminDMcg = vitaminDMcg,
            ironMg = ironMg,
            magnesiumMg = magnesiumMg,
            calciumMg = calciumMg,
            vitaminB12Mcg = vitaminB12Mcg,
            omega3G = omega3G,
            zincMg = zincMg,
            iodineMcg = iodineMcg
        )
    }
}
