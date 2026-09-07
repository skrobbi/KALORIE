package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.api.GeminiClient
import com.example.data.local.AppDatabase
import com.example.data.local.DeficitCacheEntity
import com.example.data.local.FrequentItemEntity
import com.example.data.local.MealEntity
import com.example.data.model.DeficitAnalysisInput
import com.example.data.model.DeficitAnalysisResult
import com.example.data.model.DeficitDetectedItem
import com.example.data.model.FoodItem
import com.example.data.model.GeminiMealResponse
import com.example.data.model.Micronutrients
import com.example.data.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class NutritionRepository(
    context: Context,
    private val geminiClient: GeminiClient = GeminiClient()
) {
    private val database = AppDatabase.getInstance(context)
    private val mealDao = database.mealDao()
    private val frequentItemDao = database.frequentItemDao()
    private val deficitCacheDao = database.deficitCacheDao()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val foodItemListType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
    private val foodItemAdapter = moshi.adapter<List<FoodItem>>(foodItemListType)
    private val deficitAnalysisAdapter = moshi.adapter(DeficitAnalysisResult::class.java)

    val allMeals: Flow<List<MealEntity>> = mealDao.getAllMeals()
    val allFrequentItems: Flow<List<FrequentItemEntity>> = frequentItemDao.getAllFrequentItems()
    val totalMealsCount: Flow<Int> = mealDao.getMealCount()
    val cachedMealsCount: Flow<Int> = mealDao.getCachedMealsCount()
    val frequentItemsCount: Flow<Int> = frequentItemDao.getItemsCount()

    val latestDeficitAnalysis: Flow<DeficitAnalysisResult?> =
        deficitCacheDao.getLatestDeficitAnalysis().map { entity ->
            entity?.let {
                try {
                    deficitAnalysisAdapter.fromJson(it.analysisJson)
                } catch (e: Exception) {
                    null
                }
            }
        }

    fun isGeminiKeyConfigured(): Boolean = geminiClient.isKeyConfigured

    /**
     * Core Algorithm: Check Room cache first (0 tokens) before calling Gemini API
     */
    suspend fun processMealInput(
        userInputText: String,
        bitmap: Bitmap? = null,
        forceFreshApi: Boolean = false
    ): ParseResult {
        val trimmed = userInputText.trim()
        val normalized = trimmed.lowercase()

        // 1. If NO photo and NOT forcing fresh API, check Room local cache first!
        if (bitmap == null && !forceFreshApi && trimmed.isNotBlank()) {
            val cachedItem = frequentItemDao.findByName(normalized)
                ?: frequentItemDao.searchFirst(normalized)

            if (cachedItem != null) {
                // Found in local Room cache! 0 tokens, instant response!
                frequentItemDao.incrementUsage(cachedItem.id)
                val foodItem = cachedItem.toFoodItem()
                val response = GeminiMealResponse(
                    items = listOf(foodItem),
                    clarificationChips = emptyList(),
                    totalCalories = foodItem.calories
                )
                return ParseResult.Success(response = response, isFromCache = true)
            }
        }

        // 2. Not found in cache or multimodal photo provided -> Call Gemini API
        val geminiResponse = geminiClient.parseMeal(userInputText, bitmap)

        // 3. Cache the parsed food items into frequent_items for future 0-token lookups!
        if (geminiResponse.items.isNotEmpty()) {
            for (item in geminiResponse.items) {
                val entity = FrequentItemEntity.fromFoodItem(item, if (geminiResponse.items.size == 1) trimmed else null)
                frequentItemDao.insert(entity)
            }
        }

        return ParseResult.Success(
            response = geminiResponse,
            isFromCache = false
        )
    }

    /**
     * Save confirmed meal into Room database
     */
    suspend fun saveMeal(
        mealType: String,
        title: String,
        items: List<FoodItem>,
        isFromCache: Boolean
    ): Long {
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0
        var totalMicro = Micronutrients.ZERO

        for (item in items) {
            totalCalories += item.calories
            totalProtein += item.proteinG
            totalFat += item.fatG
            totalCarbs += item.carbsG
            totalMicro = totalMicro + item.micronutrients
        }

        val jsonString = foodItemAdapter.toJson(items)

        val mealEntity = MealEntity(
            timestamp = System.currentTimeMillis(),
            mealType = mealType,
            title = title,
            itemsJson = jsonString,
            totalCalories = totalCalories,
            proteinG = totalProtein,
            fatG = totalFat,
            carbsG = totalCarbs,
            vitaminDMcg = totalMicro.vitaminDMcg,
            ironMg = totalMicro.ironMg,
            magnesiumMg = totalMicro.magnesiumMg,
            calciumMg = totalMicro.calciumMg,
            vitaminB12Mcg = totalMicro.vitaminB12Mcg,
            omega3G = totalMicro.omega3G,
            zincMg = totalMicro.zincMg,
            iodineMcg = totalMicro.iodineMcg,
            fromCache = isFromCache
        )

        return mealDao.insertMeal(mealEntity)
    }

    suspend fun deleteMeal(id: Long) {
        mealDao.deleteMealById(id)
    }

    suspend fun deleteFrequentItem(id: Long) {
        frequentItemDao.deleteById(id)
    }

    /**
     * Calculate nutrient totals for a given timeframe in days
     */
    suspend fun calculateNutrientTotals(days: Int): NutrientTotals {
        val now = System.currentTimeMillis()
        val startTime = now - (days.toLong() * 24 * 60 * 60 * 1000)
        val meals = mealDao.getMealsSinceSync(startTime)

        var calories = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0
        var micro = Micronutrients.ZERO

        for (m in meals) {
            calories += m.totalCalories
            protein += m.proteinG
            fat += m.fatG
            carbs += m.carbsG
            micro = micro + m.toMicronutrients()
        }

        return NutrientTotals(
            days = days,
            mealCount = meals.size,
            totalCalories = calories,
            proteinG = protein,
            fatG = fat,
            carbsG = carbs,
            micronutrients = micro
        )
    }

    /**
     * Module 2: Run Deficit Analysis via Gemini or WorkManager
     */
    suspend fun performDeficitAnalysis(
        days: Int,
        userProfile: UserProfile
    ): DeficitAnalysisResult {
        val periodLabel = when (days) {
            1 -> "1 день"
            3 -> "3 дня"
            7 -> "неделя"
            else -> "$days дней"
        }

        val totals = calculateNutrientTotals(days)

        // Evaluate actual intake against daily norms
        val detectedList = mutableListOf<DeficitDetectedItem>()
        val micro = totals.micronutrients

        val nutrientMap = mapOf(
            "Витамин D" to micro.vitaminDMcg,
            "Железо" to micro.ironMg,
            "Магний" to micro.magnesiumMg,
            "Кальций" to micro.calciumMg,
            "Витамин B12" to micro.vitaminB12Mcg,
            "Омега-3" to micro.omega3G,
            "Цинк" to micro.zincMg,
            "Йод" to micro.iodineMcg
        )

        for ((key, value) in nutrientMap) {
            val percent = Micronutrients.getDeficitPercentage(key, value, days)
            detectedList.add(DeficitDetectedItem(nutrient = key, percentOfNorm = percent))
        }

        // Sort by lowest % of norm to highlight biggest deficits
        detectedList.sortBy { it.percentOfNorm }

        val input = DeficitAnalysisInput(
            period = periodLabel,
            userProfile = userProfile,
            deficitsDetected = detectedList
        )

        val result = geminiClient.analyzeDeficits(input)

        // Cache result in Room
        val json = deficitAnalysisAdapter.toJson(result)
        deficitCacheDao.saveDeficitAnalysis(
            DeficitCacheEntity(
                id = 1,
                analysisJson = json,
                period = periodLabel,
                timestamp = System.currentTimeMillis()
            )
        )

        return result
    }

    sealed class ParseResult {
        data class Success(
            val response: GeminiMealResponse,
            val isFromCache: Boolean
        ) : ParseResult()

        data class Error(val message: String) : ParseResult()
    }

    data class NutrientTotals(
        val days: Int,
        val mealCount: Int,
        val totalCalories: Double,
        val proteinG: Double,
        val fatG: Double,
        val carbsG: Double,
        val micronutrients: Micronutrients
    )
}
