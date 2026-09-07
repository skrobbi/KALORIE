package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.MealEntity
import com.example.ui.screens.JournalScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMeals = listOf(
      MealEntity(
        id = 1,
        title = "Гречка с куриным филе",
        mealType = "Обед",
        totalCalories = 320.0,
        proteinG = 32.0,
        fatG = 4.5,
        carbsG = 38.0,
        fromCache = true
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        JournalScreen(
          meals = sampleMeals,
          onNavigateToAdd = {},
          onDeleteMeal = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
