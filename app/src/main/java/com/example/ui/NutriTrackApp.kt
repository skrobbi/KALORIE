package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddMealScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.AvatarBgGreen
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.PrimaryBorderGreen
import com.example.ui.theme.PrimaryContainerGreen
import com.example.ui.theme.PrimaryGreen

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    JOURNAL("Дневник", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    ADD("Добавить", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    ANALYTICS("Аналитика", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PROFILE("Профиль", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun NutriTrackApp(
    viewModel: NutritionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(AppDestination.JOURNAL) }

    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val addMealState by viewModel.addMealState.collectAsStateWithLifecycle()
    val latestAnalysis by viewModel.latestDeficitAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingDeficits.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedAnalysisPeriod.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val frequentItems by viewModel.allFrequentItems.collectAsStateWithLifecycle()
    val totalMealsCount by viewModel.totalMealsCount.collectAsStateWithLifecycle()
    val cachedMealsCount by viewModel.cachedMealsCount.collectAsStateWithLifecycle()

    val userName = if (userProfile.name.isNotBlank()) userProfile.name else "Алекс"
    val avatarInitial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "А"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ПРИВЕТ, ${userName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "NutriTrack",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        // Sleek round user avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AvatarBgGreen)
                                .clickable { currentDestination = AppDestination.PROFILE }
                                .testTag("header_avatar"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarInitial,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = CardBorderLight
                    )
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = CardBorderLight
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    AppDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title.uppercase(),
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryGreen,
                                selectedTextColor = PrimaryGreen,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                indicatorColor = PrimaryContainerGreen
                            ),
                            modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    AppDestination.JOURNAL -> {
                        JournalScreen(
                            meals = allMeals,
                            onNavigateToAdd = { currentDestination = AppDestination.ADD },
                            onDeleteMeal = { id -> viewModel.deleteMeal(id) }
                        )
                    }
                    AppDestination.ADD -> {
                        AddMealScreen(
                            state = addMealState,
                            onInputTextChanged = { text -> viewModel.onInputTextChanged(text) },
                            onBitmapSelected = { bitmap -> viewModel.onBitmapSelected(bitmap) },
                            onParseMeal = { forceFresh -> viewModel.parseMeal(forceFresh) },
                            onClarificationChipClicked = { chip -> viewModel.onClarificationChipClicked(chip) },
                            onSaveMeal = { mealType ->
                                viewModel.saveCurrentMeal(mealType)
                                currentDestination = AppDestination.JOURNAL
                            },
                            onClearDraft = { viewModel.clearAddMealDraft() }
                        )
                    }
                    AppDestination.ANALYTICS -> {
                        AnalyticsScreen(
                            analysisResult = latestAnalysis,
                            isAnalyzing = isAnalyzing,
                            selectedPeriodDays = selectedPeriod,
                            userProfile = userProfile,
                            onPeriodSelected = { days -> viewModel.setAnalysisPeriod(days) },
                            onRefreshAnalysis = { viewModel.runDeficitAnalysis() }
                        )
                    }
                    AppDestination.PROFILE -> {
                        ProfileScreen(
                            userProfile = userProfile,
                            totalMeals = totalMealsCount,
                            cachedMeals = cachedMealsCount,
                            frequentItems = frequentItems,
                            isApiKeyConfigured = viewModel.isGeminiKeyConfigured,
                            onProfileUpdated = { profile -> viewModel.updateUserProfile(profile) },
                            onDeleteFrequentItem = { id -> viewModel.deleteFrequentItem(id) }
                        )
                    }
                }
            }
        }
    }
}
