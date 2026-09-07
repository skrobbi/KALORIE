package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Micronutrients
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.DeficitOrange
import com.example.ui.theme.DeficitRed
import com.example.ui.theme.OptimalGreen
import com.example.ui.theme.PrimaryBorderGreen
import com.example.ui.theme.PrimaryContainerGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.CalorieOrange
import com.example.ui.theme.CarbsPurple
import com.example.ui.theme.FatGold
import com.example.ui.theme.ProteinBlue

@Composable
fun CacheBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier.testTag("cache_badge"),
        shape = RoundedCornerShape(50),
        color = PrimaryContainerGreen,
        border = BorderStroke(1.dp, PrimaryBorderGreen),
        contentColor = PrimaryGreen
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Кэш Room",
                modifier = Modifier.size(13.dp),
                tint = PrimaryGreen
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (compact) "0 токенов" else "Кэш Room • 0 токенов",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

/**
 * Sleek 4-column quick glance metric grid as designed in the Sleek Interface theme.
 */
@Composable
fun SleekQuickNutrientsGrid(
    micronutrients: Micronutrients,
    daysMultiplier: Int = 1,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Pair("вит. D", micronutrients.vitaminDMcg),
        Pair("Fe", micronutrients.ironMg),
        Pair("Mg", micronutrients.magnesiumMg),
        Pair("Ca", micronutrients.calciumMg)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleek_quick_nutrients_grid"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (label, value) ->
            val nutrientFullName = when (label) {
                "вит. D" -> "Витамин D"
                "Fe" -> "Железо"
                "Mg" -> "Магний"
                "Ca" -> "Кальций"
                else -> label
            }
            val percent = Micronutrients.getDeficitPercentage(nutrientFullName, value, daysMultiplier)
            val percentColor = when {
                percent < 35 -> DeficitRed
                percent < 70 -> DeficitOrange
                else -> PrimaryGreen
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_stat_${label}"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, CardBorderLight),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$percent%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = percentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MacroSummaryCard(
    calories: Double,
    protein: Double,
    fat: Double,
    carbs: Double,
    targetCalories: Double = 2200.0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("macro_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, CardBorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "КАЛОРИИ ЗА ДЕНЬ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${calories.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = CalorieOrange
                        )
                        Text(
                            text = " / ${targetCalories.toInt()} ккал",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }
                }

                // Progress circular gauge percentage
                val progress = (calories / targetCalories).coerceIn(0.0, 1.5).toFloat()
                Box(contentAlignment = Alignment.Center) {
                    val percent = (progress * 100).toInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (percent > 105) CalorieOrange else PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (calories / targetCalories).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CalorieOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Macros Row (Protein, Fat, Carbs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem(
                    label = "Белки",
                    amount = protein,
                    color = ProteinBlue,
                    target = 110.0
                )
                MacroItem(
                    label = "Жиры",
                    amount = fat,
                    color = FatGold,
                    target = 70.0
                )
                MacroItem(
                    label = "Углеводы",
                    amount = carbs,
                    color = CarbsPurple,
                    target = 250.0
                )
            }
        }
    }
}

@Composable
fun MacroItem(
    label: String,
    amount: Double,
    color: Color,
    target: Double
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${amount.toInt()} г",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "из ${target.toInt()} г",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun MicronutrientsSection(
    micronutrients: Micronutrients,
    daysMultiplier: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top 4-column quick glance
        SleekQuickNutrientsGrid(
            micronutrients = micronutrients,
            daysMultiplier = daysMultiplier
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("micronutrients_section"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, CardBorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ТОП-8 микронутриентов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Мониторинг ключевых дефицитов",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val items = listOf(
                    Triple("Витамин D", micronutrients.vitaminDMcg, "мкг"),
                    Triple("Железо", micronutrients.ironMg, "мг"),
                    Triple("Магний", micronutrients.magnesiumMg, "мг"),
                    Triple("Кальций", micronutrients.calciumMg, "мг"),
                    Triple("Витамин B12", micronutrients.vitaminB12Mcg, "мкг"),
                    Triple("Омега-3", micronutrients.omega3G, "г"),
                    Triple("Цинк", micronutrients.zincMg, "мг"),
                    Triple("Йод", micronutrients.iodineMcg, "мкг")
                )

                for ((name, value, unit) in items) {
                    val percent = Micronutrients.getDeficitPercentage(name, value, daysMultiplier)
                    val statusColor = when {
                        percent < 35 -> DeficitRed
                        percent < 70 -> DeficitOrange
                        else -> PrimaryGreen
                    }
                    val isDeficit = percent < 50

                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${String.format("%.1f", value)} $unit ($percent%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (isDeficit) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Дефицит",
                                        tint = statusColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Норма",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClarificationChipsSection(
    chips: List<String>,
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = chips.isNotEmpty(),
        enter = fadeIn() + slideInVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("clarification_chips_section")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Уточните вариант блюда:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEachIndexed { index, chipText ->
                    Surface(
                        onClick = { onChipClicked(chipText) },
                        shape = RoundedCornerShape(50),
                        color = PrimaryContainerGreen,
                        border = BorderStroke(1.dp, PrimaryBorderGreen),
                        modifier = Modifier.testTag("clarification_chip_$index")
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

