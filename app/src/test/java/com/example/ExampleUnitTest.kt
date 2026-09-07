package com.example

import com.example.data.local.FrequentItemEntity
import com.example.data.model.FoodItem
import com.example.data.model.Micronutrients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDeficitPercentageCalculation() {
    // Vitamin D daily norm is 15 mcg
    val halfPercent = Micronutrients.getDeficitPercentage("Витамин D", 7.5, 1)
    assertEquals(50, halfPercent)

    val fullPercent = Micronutrients.getDeficitPercentage("Витамин D", 15.0, 1)
    assertEquals(100, fullPercent)

    // For 3 days, target is 45 mcg
    val threeDayHalf = Micronutrients.getDeficitPercentage("Витамин D", 22.5, 3)
    assertEquals(50, threeDayHalf)
  }

  @Test
  fun testFoodItemToFrequentEntity() {
    val food = FoodItem(
      name = "Яблоко зеленое",
      estimatedWeightG = 150.0,
      calories = 78.0,
      proteinG = 0.4,
      fatG = 0.2,
      carbsG = 20.7,
      micronutrients = Micronutrients(ironMg = 0.2)
    )

    val entity = FrequentItemEntity.fromFoodItem(food)
    assertEquals("яблоко зеленое", entity.normalizedName)
    assertEquals(78.0, entity.calories, 0.01)

    val backToFood = entity.toFoodItem()
    assertEquals(food.name, backToFood.name)
    assertEquals(0.2, backToFood.micronutrients.ironMg, 0.01)
  }
}
