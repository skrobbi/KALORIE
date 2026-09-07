package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FrequentItemEntity
import com.example.data.local.MealEntity
import com.example.data.model.DeficitAnalysisResult
import com.example.data.model.FoodItem
import com.example.data.model.GeminiMealResponse
import com.example.data.model.Micronutrients
import com.example.data.model.UserProfile
import com.example.data.repository.NutritionRepository
import com.example.worker.DailyNutritionAnalysisWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddMealUiState(
    val inputText: String = "",
    val selectedBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val parsedResponse: GeminiMealResponse? = null,
    val isFromCache: Boolean = false,
    val clarificationChips: List<String> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NutritionRepository(application.applicationContext)

    init {
        // Ensure background WorkManager schedule is initialized
        DailyNutritionAnalysisWorker.schedule(application.applicationContext)
    }

    val isGeminiKeyConfigured: Boolean = repository.isGeminiKeyConfigured()

    val allMeals: StateFlow<List<MealEntity>> = repository.allMeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFrequentItems: StateFlow<List<FrequentItemEntity>> = repository.allFrequentItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMealsCount: StateFlow<Int> = repository.totalMealsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cachedMealsCount: StateFlow<Int> = repository.cachedMealsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestDeficitAnalysis: StateFlow<DeficitAnalysisResult?> = repository.latestDeficitAnalysis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _userProfile = MutableStateFlow(
        UserProfile(
            diet = "обычная",
            allergiesOrIntolerance = listOf("лактоза"),
            dislikedFoods = listOf("печень", "брокколи"),
            budgetPreference = "экономные простые продукты"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _addMealState = MutableStateFlow(AddMealUiState())
    val addMealState: StateFlow<AddMealUiState> = _addMealState.asStateFlow()

    private val _isAnalyzingDeficits = MutableStateFlow(false)
    val isAnalyzingDeficits: StateFlow<Boolean> = _isAnalyzingDeficits.asStateFlow()

    private val _selectedAnalysisPeriod = MutableStateFlow(3)
    val selectedAnalysisPeriod: StateFlow<Int> = _selectedAnalysisPeriod.asStateFlow()

    fun onInputTextChanged(text: String) {
        _addMealState.value = _addMealState.value.copy(
            inputText = text,
            errorMessage = null
        )
    }

    fun onBitmapSelected(bitmap: Bitmap?) {
        _addMealState.value = _addMealState.value.copy(
            selectedBitmap = bitmap,
            errorMessage = null
        )
    }

    fun clearAddMealDraft() {
        _addMealState.value = AddMealUiState()
    }

    /**
     * Parse meal: Checks Room cache first (0 tokens) before calling Gemini API
     */
    fun parseMeal(forceFreshApi: Boolean = false) {
        val currentState = _addMealState.value
        val text = currentState.inputText.trim()
        val bitmap = currentState.selectedBitmap

        if (text.isBlank() && bitmap == null) {
            _addMealState.value = currentState.copy(
                errorMessage = "Пожалуйста, введите название блюда или прикрепите фото"
            )
            return
        }

        viewModelScope.launch {
            _addMealState.value = currentState.copy(
                isLoading = true,
                errorMessage = null,
                statusMessage = if (bitmap == null && !forceFreshApi) "Проверка локального кэша Room..." else "Анализ блюда через Gemini ИИ..."
            )

            try {
                val result = repository.processMealInput(
                    userInputText = text,
                    bitmap = bitmap,
                    forceFreshApi = forceFreshApi
                )

                when (result) {
                    is NutritionRepository.ParseResult.Success -> {
                        val message = if (result.isFromCache) {
                            "⚡ Мгновенно найдено в локальном кэше Room (0 токенов!)"
                        } else {
                            "✨ Блюдо успешно проанализировано через Gemini API"
                        }

                        _addMealState.value = _addMealState.value.copy(
                            isLoading = false,
                            parsedResponse = result.response,
                            isFromCache = result.isFromCache,
                            clarificationChips = result.response.clarificationChips,
                            statusMessage = message
                        )
                    }
                    is NutritionRepository.ParseResult.Error -> {
                        _addMealState.value = _addMealState.value.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _addMealState.value = _addMealState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка при распознавании: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * User clicks on clarification chip (e.g. "С молоком и сахаром" or "Сыр и масло")
     * As specified in TZ:
     * Пользователь нажимает «С молоком и сахаром» — приложение просто добавляет этот текст к предыдущему и отправляет повторный короткий запрос.
     */
    fun onClarificationChipClicked(chip: String) {
        val currentText = _addMealState.value.inputText.trim()
        val newText = if (currentText.isBlank()) chip else "$currentText, $chip"
        _addMealState.value = _addMealState.value.copy(
            inputText = newText,
            clarificationChips = emptyList() // clear chips and re-query
        )
        parseMeal(forceFreshApi = true)
    }

    /**
     * Save the parsed meal to daily log
     */
    fun saveCurrentMeal(mealType: String = "Прием пищи") {
        val parsed = _addMealState.value.parsedResponse ?: return
        val isCache = _addMealState.value.isFromCache
        val title = if (_addMealState.value.inputText.isNotBlank()) {
            _addMealState.value.inputText.trim()
        } else {
            parsed.items.joinToString(", ") { it.name }
        }

        viewModelScope.launch {
            repository.saveMeal(
                mealType = mealType,
                title = title,
                items = parsed.items,
                isFromCache = isCache
            )
            // Reset add form
            _addMealState.value = AddMealUiState(
                statusMessage = "Блюдо успешно сохранено в дневник!"
            )
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun deleteFrequentItem(id: Long) {
        viewModelScope.launch {
            repository.deleteFrequentItem(id)
        }
    }

    fun setAnalysisPeriod(days: Int) {
        _selectedAnalysisPeriod.value = days
        runDeficitAnalysis(days)
    }

    fun runDeficitAnalysis(days: Int = _selectedAnalysisPeriod.value) {
        viewModelScope.launch {
            _isAnalyzingDeficits.value = true
            try {
                repository.performDeficitAnalysis(days, _userProfile.value)
            } catch (e: Exception) {
                // Handled gracefully in repository fallback
            } finally {
                _isAnalyzingDeficits.value = false
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        // Rerun analysis with new restrictions
        runDeficitAnalysis(_selectedAnalysisPeriod.value)
    }
}
