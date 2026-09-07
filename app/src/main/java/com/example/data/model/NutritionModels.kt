package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Micronutrients(
    @Json(name = "vitamin_d_mcg") val vitaminDMcg: Double = 0.0,
    @Json(name = "iron_mg") val ironMg: Double = 0.0,
    @Json(name = "magnesium_mg") val magnesiumMg: Double = 0.0,
    @Json(name = "calcium_mg") val calciumMg: Double = 0.0,
    @Json(name = "vitamin_b12_mcg") val vitaminB12Mcg: Double = 0.0,
    @Json(name = "omega3_g") val omega3G: Double = 0.0,
    @Json(name = "zinc_mg") val zincMg: Double = 0.0,
    @Json(name = "iodine_mcg") val iodineMcg: Double = 0.0
) {
    operator fun plus(other: Micronutrients): Micronutrients {
        return Micronutrients(
            vitaminDMcg = this.vitaminDMcg + other.vitaminDMcg,
            ironMg = this.ironMg + other.ironMg,
            magnesiumMg = this.magnesiumMg + other.magnesiumMg,
            calciumMg = this.calciumMg + other.calciumMg,
            vitaminB12Mcg = this.vitaminB12Mcg + other.vitaminB12Mcg,
            omega3G = this.omega3G + other.omega3G,
            zincMg = this.zincMg + other.zincMg,
            iodineMcg = this.iodineMcg + other.iodineMcg
        )
    }

    companion object {
        val ZERO = Micronutrients()

        // Daily recommended intake standards for adults
        val DAILY_NORMS = mapOf(
            "Витамин D" to Pair(15.0, "мкг"),
            "Железо" to Pair(14.0, "мг"),
            "Магний" to Pair(400.0, "мг"),
            "Кальций" to Pair(1000.0, "мг"),
            "Витамин B12" to Pair(2.4, "мкг"),
            "Омега-3" to Pair(1.6, "г"),
            "Цинк" to Pair(11.0, "мг"),
            "Йод" to Pair(150.0, "мкг")
        )

        fun getDeficitPercentage(nutrientKey: String, actualValue: Double, daysMultiplier: Int = 1): Int {
            val normPair = DAILY_NORMS[nutrientKey] ?: return 100
            val target = normPair.first * daysMultiplier
            if (target <= 0.0) return 100
            val percent = ((actualValue / target) * 100).toInt()
            return percent.coerceIn(0, 200)
        }
    }
}

@JsonClass(generateAdapter = true)
data class FoodItem(
    @Json(name = "name") val name: String,
    @Json(name = "estimated_weight_g") val estimatedWeightG: Double = 100.0,
    @Json(name = "calories") val calories: Double = 0.0,
    @Json(name = "protein_g") val proteinG: Double = 0.0,
    @Json(name = "fat_g") val fatG: Double = 0.0,
    @Json(name = "carbs_g") val carbsG: Double = 0.0,
    @Json(name = "micronutrients") val micronutrients: Micronutrients = Micronutrients.ZERO
)

@JsonClass(generateAdapter = true)
data class GeminiMealResponse(
    @Json(name = "items") val items: List<FoodItem> = emptyList(),
    @Json(name = "clarification_chips") val clarificationChips: List<String> = emptyList(),
    @Json(name = "total_calories") val totalCalories: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class PrimaryDeficit(
    @Json(name = "nutrient") val nutrient: String,
    @Json(name = "status") val status: String,
    @Json(name = "why_matters") val whyMatters: String
)

@JsonClass(generateAdapter = true)
data class QuickFixSnack(
    @Json(name = "snack_name") val snackName: String,
    @Json(name = "benefit") val benefit: String
)

@JsonClass(generateAdapter = true)
data class HomeMealIdea(
    @Json(name = "title") val title: String,
    @Json(name = "budget_friendly") val budgetFriendly: Boolean = true,
    @Json(name = "why_fits") val whyFits: String,
    @Json(name = "simple_recipe") val simpleRecipe: String
)

@JsonClass(generateAdapter = true)
data class DeficitAnalysisResult(
    @Json(name = "primary_deficits") val primaryDeficits: List<PrimaryDeficit> = emptyList(),
    @Json(name = "quick_fix_snack") val quickFixSnack: QuickFixSnack? = null,
    @Json(name = "home_meal_idea") val homeMealIdea: HomeMealIdea? = null,
    val period: String = "1 день",
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val name: String = "Алекс",
    val diet: String = "обычная",
    val allergiesOrIntolerance: List<String> = listOf("лактоза"),
    val dislikedFoods: List<String> = listOf("печень", "брокколи"),
    val budgetPreference: String = "экономные простые продукты"
)

data class DeficitDetectedItem(
    val nutrient: String,
    val percentOfNorm: Int
)

data class DeficitAnalysisInput(
    val period: String,
    val userProfile: UserProfile,
    val deficitsDetected: List<DeficitDetectedItem>
)
