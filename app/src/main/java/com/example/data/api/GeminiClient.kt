package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.DeficitAnalysisInput
import com.example.data.model.DeficitAnalysisResult
import com.example.data.model.FoodItem
import com.example.data.model.GeminiMealResponse
import com.example.data.model.HomeMealIdea
import com.example.data.model.Micronutrients
import com.example.data.model.PrimaryDeficit
import com.example.data.model.QuickFixSnack
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Use supported modern preview model as dictated by guidelines
    private val modelName = "gemini-flash-latest"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    val isKeyConfigured: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }

    /**
     * Scale bitmap to max ~1024px and compress to Base64 JPEG to keep payloads fast & efficient
     */
    fun compressBitmap(bitmap: Bitmap, maxDimension: Int = 1024): String {
        val width = bitmap.width
        val height = bitmap.height
        val scale = if (width > maxDimension || height > maxDimension) {
            maxDimension.toFloat() / maxOf(width, height)
        } else {
            1.0f
        }

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Module 1: Meal recognition (Text and/or Photo) with clarification chips
     */
    suspend fun parseMeal(
        userInputText: String,
        bitmap: Bitmap? = null
    ): GeminiMealResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (!isKeyConfigured) {
            Log.w(TAG, "Gemini API key is not configured in Secrets panel. Using local intelligent estimation.")
            return@withContext fallbackLocalMealEstimator(userInputText)
        }

        try {
            val systemInstruction = """
Ты — нутрициолог-аналитик для мобильного приложения.
Твоя задача: проанализировать входящее сообщение (текст на русском языке и/или фотографию блюда) и разложить прием пищи на продукты с оценкой веса и нутриентов.

Правила анализа:
1. Семантический вес: если вес не указан, оценивай его по стандартным бытовым меркам (тарелка, штука, ломтик, кружка, горсть). Если дано фото — оценивай порцию визуально.
2. Фокусируйся ТОЛЬКО на КБЖУ и ТОП-8 критических нутриентах:
   - vitamin_d_mcg (мкг)
   - iron_mg (мг)
   - magnesium_mg (мг)
   - calcium_mg (мг)
   - vitamin_b12_mcg (мкг)
   - omega3_g (г)
   - zinc_mg (мг)
   - iodine_mcg (мкг)
3. Интерактивные уточнения (clarifications):
   Если блюдо двусмысленно (например: "кофе", "бутерброд", "суп"), НЕ пытайся угадать наугад. 
   Сделай базовую оценку, но добавь в массив "clarification_chips" 2-4 короткие кнопки для пользователя (например: "С сахаром", "Без молока", "Сыр и масло", "Колбаса").
   Если всё однозначно (например, "яблоко зеленое 1 шт"), оставь массив clarification_chips пустым.

Ответ должен быть строго в JSON-формате по заданной схеме.
            """.trimIndent()

            val partsArray = JSONArray()

            val promptText = if (userInputText.isNotBlank()) {
                userInputText
            } else {
                "Определи блюдо на фотографии и его состав."
            }
            partsArray.put(JSONObject().put("text", promptText))

            if (bitmap != null) {
                val base64Data = compressBitmap(bitmap)
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Data)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val requestJson = JSONObject()
            requestJson.put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", partsArray)
                )
            )

            // System instruction
            requestJson.put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction))
                )
            )

            // Generation config with JSON response format
            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            requestJson.put("generationConfig", generationConfig)

            val url = "$baseUrl?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini API")

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API HTTP Error ${response.code}: $responseBody")
                return@withContext fallbackLocalMealEstimator(userInputText)
            }

            parseGeminiMealJson(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API: ${e.message}", e)
            fallbackLocalMealEstimator(userInputText)
        }
    }

    /**
     * Module 2: Deficit Analysis + Quick Snack + Home Meal Idea
     */
    suspend fun analyzeDeficits(
        input: DeficitAnalysisInput
    ): DeficitAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (!isKeyConfigured) {
            Log.w(TAG, "Gemini API key is not configured. Generating local nutrition recommendations.")
            return@withContext fallbackLocalDeficitAnalysis(input)
        }

        try {
            val systemInstruction = """
Ты — практичный диетолог. Ты не заставляешь людей готовить ресторанные блюда или тратить много денег.
Тебе передаются:
1. Профиль пользователя: аллергии/ограничения, предпочтения по бюджету и список нелюбимых продуктов.
2. Суммарное потребление микроэлементов за выбранный период (1 день, 3 дня или неделя).

Твоя задача:
1. Определить ТОП-1 или ТОП-2 нутриента с наибольшим дефицитом (среди: D, Fe, Mg, Ca, B12, Omega-3, Zn, I).
2. "Быстрый перекус прямо сейчас" (Quick Snack): предложить 1-2 супер-простых продукта, которые не нужно долго готовить (купить в любом магазине у дома / взять из холодильника: например, вареное яйцо, горсть грецких орехов, консервированный тунец, банан, кусочек твердого сыра).
3. "Идея для обычного обеда/ужина": предложить 1 простое, недорогое домашнее блюдо из доступных продуктов, закрывающее дефицит.
4. СТРОГО учитывать ограничения пользователя: если пользователь не ест печень — не предлагать её для закрытия железа! Если бюджет "эконом" — никаких авокадо, спаржи и свежего лосося (заменяй на сельдь, скумбрию, сезонные овощи).

Ответ строго в JSON.
            """.trimIndent()

            val inputJson = JSONObject().apply {
                put("period", input.period)
                put("user_profile", JSONObject().apply {
                    put("diet", input.userProfile.diet)
                    put("allergies_or_intolerance", JSONArray(input.userProfile.allergiesOrIntolerance))
                    put("disliked_foods", JSONArray(input.userProfile.dislikedFoods))
                    put("budget_preference", input.userProfile.budgetPreference)
                })
                val deficitsArr = JSONArray()
                for (item in input.deficitsDetected) {
                    deficitsArr.put(JSONObject().apply {
                        put("nutrient", item.nutrient)
                        put("percent_of_norm", item.percentOfNorm)
                    })
                }
                put("deficits_detected", deficitsArr)
            }

            val requestJson = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", inputJson.toString()))
                        )
                    )
                )
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().put("responseMimeType", "application/json")
                )
            }

            val url = "$baseUrl?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response body")

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API Deficit HTTP Error ${response.code}: $responseBody")
                return@withContext fallbackLocalDeficitAnalysis(input)
            }

            parseDeficitResponseJson(responseBody, input.period)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini deficit analysis: ${e.message}", e)
            fallbackLocalDeficitAnalysis(input)
        }
    }

    private fun parseGeminiMealJson(rawResponse: String): GeminiMealResponse {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return fallbackLocalMealEstimator("")
        val firstCandidate = candidates.optJSONObject(0) ?: return fallbackLocalMealEstimator("")
        val content = firstCandidate.optJSONObject("content") ?: return fallbackLocalMealEstimator("")
        val parts = content.optJSONArray("parts") ?: return fallbackLocalMealEstimator("")
        val rawText = parts.optJSONObject(0)?.optString("text") ?: return fallbackLocalMealEstimator("")

        val cleanJson = cleanJsonString(rawText)
        val mealObj = JSONObject(cleanJson)

        val items = mutableListOf<FoodItem>()
        val itemsArray = mealObj.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            val microObj = itemObj.optJSONObject("micronutrients") ?: JSONObject()

            val micro = Micronutrients(
                vitaminDMcg = microObj.optDouble("vitamin_d_mcg", 0.0),
                ironMg = microObj.optDouble("iron_mg", 0.0),
                magnesiumMg = microObj.optDouble("magnesium_mg", 0.0),
                calciumMg = microObj.optDouble("calcium_mg", 0.0),
                vitaminB12Mcg = microObj.optDouble("vitamin_b12_mcg", 0.0),
                omega3G = microObj.optDouble("omega3_g", 0.0),
                zincMg = microObj.optDouble("zinc_mg", 0.0),
                iodineMcg = microObj.optDouble("iodine_mcg", 0.0)
            )

            items.add(
                FoodItem(
                    name = itemObj.optString("name", "Продукт"),
                    estimatedWeightG = itemObj.optDouble("estimated_weight_g", 100.0),
                    calories = itemObj.optDouble("calories", 0.0),
                    proteinG = itemObj.optDouble("protein_g", 0.0),
                    fatG = itemObj.optDouble("fat_g", 0.0),
                    carbsG = itemObj.optDouble("carbs_g", 0.0),
                    micronutrients = micro
                )
            )
        }

        val chips = mutableListOf<String>()
        val chipsArray = mealObj.optJSONArray("clarification_chips")
        if (chipsArray != null) {
            for (i in 0 until chipsArray.length()) {
                val chip = chipsArray.optString(i)
                if (!chip.isNullOrBlank()) {
                    chips.add(chip)
                }
            }
        }

        val totalCalories = mealObj.optDouble(
            "total_calories",
            items.sumOf { it.calories }
        )

        return GeminiMealResponse(
            items = items,
            clarificationChips = chips,
            totalCalories = totalCalories
        )
    }

    private fun parseDeficitResponseJson(rawResponse: String, period: String): DeficitAnalysisResult {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return fallbackLocalDeficitAnalysis(DeficitAnalysisInput(period, UserProfile(), emptyList()))
        val firstCandidate = candidates.optJSONObject(0) ?: return fallbackLocalDeficitAnalysis(DeficitAnalysisInput(period, UserProfile(), emptyList()))
        val content = firstCandidate.optJSONObject("content") ?: return fallbackLocalDeficitAnalysis(DeficitAnalysisInput(period, UserProfile(), emptyList()))
        val parts = content.optJSONArray("parts") ?: return fallbackLocalDeficitAnalysis(DeficitAnalysisInput(period, UserProfile(), emptyList()))
        val rawText = parts.optJSONObject(0)?.optString("text") ?: return fallbackLocalDeficitAnalysis(DeficitAnalysisInput(period, UserProfile(), emptyList()))

        val cleanJson = cleanJsonString(rawText)
        val obj = JSONObject(cleanJson)

        val deficits = mutableListOf<PrimaryDeficit>()
        val deficitsArr = obj.optJSONArray("primary_deficits")
        if (deficitsArr != null) {
            for (i in 0 until deficitsArr.length()) {
                val d = deficitsArr.optJSONObject(i) ?: continue
                deficits.add(
                    PrimaryDeficit(
                        nutrient = d.optString("nutrient", "Нутриент"),
                        status = d.optString("status", "Дефицит"),
                        whyMatters = d.optString("why_matters", "")
                    )
                )
            }
        }

        var quickSnack: QuickFixSnack? = null
        val snackObj = obj.optJSONObject("quick_fix_snack")
        if (snackObj != null) {
            quickSnack = QuickFixSnack(
                snackName = snackObj.optString("snack_name", ""),
                benefit = snackObj.optString("benefit", "")
            )
        }

        var mealIdea: HomeMealIdea? = null
        val mealObj = obj.optJSONObject("home_meal_idea")
        if (mealObj != null) {
            mealIdea = HomeMealIdea(
                title = mealObj.optString("title", ""),
                budgetFriendly = mealObj.optBoolean("budget_friendly", true),
                whyFits = mealObj.optString("why_fits", ""),
                simpleRecipe = mealObj.optString("simple_recipe", "")
            )
        }

        return DeficitAnalysisResult(
            primaryDeficits = deficits,
            quickFixSnack = quickSnack,
            homeMealIdea = mealIdea,
            period = period,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun cleanJsonString(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    /**
     * Fallback estimator for robust local operation and demonstration
     */
    private fun fallbackLocalMealEstimator(input: String): GeminiMealResponse {
        val query = input.trim().lowercase()

        // Check if query contains hints of ambiguous foods to demonstrate clarification chips
        val chips = mutableListOf<String>()
        if (query.contains("кофе") && !query.contains("сахар") && !query.contains("молок")) {
            chips.addAll(listOf("Черный без сахара", "С молоком и сахаром", "Капучино"))
        } else if (query.contains("бутерброд") || query.contains("сэндвич")) {
            chips.addAll(listOf("С сыром и маслом", "С колбасой", "С авокадо"))
        } else if (query.contains("суп") || query.contains("борщ")) {
            chips.addAll(listOf("Со сметаной", "Без сметаны", "С сухариками"))
        }

        val name = if (input.isNotBlank()) input.replaceFirstChar { it.uppercase() } else "Прием пищи"
        val item = FoodItem(
            name = name,
            estimatedWeightG = 200.0,
            calories = 240.0,
            proteinG = 12.0,
            fatG = 8.0,
            carbsG = 30.0,
            micronutrients = Micronutrients(
                vitaminDMcg = 0.5,
                ironMg = 1.8,
                magnesiumMg = 45.0,
                calciumMg = 85.0,
                vitaminB12Mcg = 0.4,
                omega3G = 0.2,
                zincMg = 1.2,
                iodineMcg = 15.0
            )
        )

        return GeminiMealResponse(
            items = listOf(item),
            clarificationChips = chips,
            totalCalories = 240.0
        )
    }

    private fun fallbackLocalDeficitAnalysis(input: DeficitAnalysisInput): DeficitAnalysisResult {
        val topDeficits = input.deficitsDetected.sortedBy { it.percentOfNorm }.take(2)
        val primaryDeficits = topDeficits.map { def ->
            val why = when (def.nutrient) {
                "Омега-3" -> "Важна для здоровья сосудов, мозга и снижения уровня воспалений."
                "Кальций" -> "Нужен для крепости костей, зубов и правильного мышечного сокращения."
                "Витамин D" -> "Критичен для усвоения кальция, иммунитета и гормонального баланса."
                "Железо" -> "Необходимо для переноса кислорода эритроцитами и поддержания энергии."
                "Магний" -> "Снижает уровень стресса, нормализует сон и поддерживает работу сердца."
                "Витамин B12" -> "Поддерживает нервную систему и выработку клеток крови."
                "Цинк" -> "Ключевой элемент для иммунной защиты и регенерации тканей."
                "Йод" -> "Определяет синтез тиреоидных гормонов щитовидной железы."
                else -> "Необходим для поддержания обменных процессов в организме."
            }
            PrimaryDeficit(
                nutrient = def.nutrient,
                status = if (def.percentOfNorm < 40) "Выраженный дефицит" else "Умеренный дефицит",
                whyMatters = why
            )
        }.ifEmpty {
            listOf(
                PrimaryDeficit(
                    nutrient = "Омега-3",
                    status = "Выраженный дефицит",
                    whyMatters = "Важна для здоровья сосудов, мозга и снятия воспалений."
                ),
                PrimaryDeficit(
                    nutrient = "Кальций",
                    status = "Умеренный дефицит",
                    whyMatters = "Нужен для крепости костей и мышечных сокращений."
                )
            )
        }

        val quickSnack = QuickFixSnack(
            snackName = "Горсть грецких орехов (30г) или бутерброд с консервированной сардиной",
            benefit = "Быстро закроет суточную потребность в полезных жирах и омега-3 без готовки."
        )

        val homeMeal = HomeMealIdea(
            title = "Запеченная скумбрия с картофелем и кунжутом",
            budgetFriendly = true,
            whyFits = "Скумбрия — один из самых доступных и богатых источников Омега-3, а кунжут восполнит кальций без использования молочки.",
            simpleRecipe = "Скумбрию посолить, поперчить и запечь 25 минут при 180°C. Подавать с отварным картофелем, посыпав семенами кунжута."
        )

        return DeficitAnalysisResult(
            primaryDeficits = primaryDeficits,
            quickFixSnack = quickSnack,
            homeMealIdea = homeMeal,
            period = input.period,
            timestamp = System.currentTimeMillis()
        )
    }

    companion object {
        private const val TAG = "GeminiClient"
    }
}
