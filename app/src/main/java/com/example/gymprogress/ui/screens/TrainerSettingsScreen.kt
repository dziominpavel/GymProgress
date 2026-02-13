package com.example.gymprogress.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.ProgressionType
import com.example.gymprogress.data.SplitType
import com.example.gymprogress.data.TrainerSettings
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerSettingsScreen(
    settings: TrainerSettings,
    onSettingsChanged: (TrainerSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSettings by remember(settings) { mutableStateOf(settings) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onSettingsChanged(currentSettings)
                    onBack()
                }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "ТРЕНЕР",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt)
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Тип сплита ──
            SectionTitle("Тип сплита")
            SectionSubtitle("Определяет набор мышечных групп на каждую тренировку")
            Spacer(modifier = Modifier.height(Spacing.sm))

            SplitType.entries.forEach { split ->
                val isSelected = split == currentSettings.splitType
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(CardShape)
                        .clickable {
                            currentSettings = currentSettings.copy(splitType = split)
                            onSettingsChanged(currentSettings)
                        }
                        .then(
                            if (isSelected) Modifier.border(
                                1.5.dp, Volt.copy(alpha = 0.5f), CardShape
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Volt.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = CardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                currentSettings = currentSettings.copy(splitType = split)
                                onSettingsChanged(currentSettings)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Volt)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = split.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Volt
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = split.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Дней в неделю ──
            SectionTitle("Дней в неделю: ${currentSettings.daysPerWeek}")
            SectionSubtitle("Сколько раз в неделю вы тренируетесь")
            Spacer(modifier = Modifier.height(Spacing.xs))

            Slider(
                value = currentSettings.daysPerWeek.toFloat(),
                onValueChange = {
                    currentSettings = currentSettings.copy(daysPerWeek = it.toInt())
                },
                onValueChangeFinished = {
                    onSettingsChanged(currentSettings)
                },
                valueRange = 2f..6f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = Volt,
                    activeTrackColor = Volt
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (2..6).forEach {
                    Text(
                        "$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Тип прогрессии ──
            SectionTitle("Тип прогрессии")
            SectionSubtitle("Как увеличивать нагрузку")
            Spacer(modifier = Modifier.height(Spacing.sm))

            ProgressionType.entries.forEach { prog ->
                val isSelected = prog == currentSettings.progressionType
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(CardShape)
                        .clickable {
                            currentSettings = currentSettings.copy(progressionType = prog)
                            onSettingsChanged(currentSettings)
                        }
                        .then(
                            if (isSelected) Modifier.border(
                                1.5.dp, Volt.copy(alpha = 0.5f), CardShape
                            ) else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Volt.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = CardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                currentSettings = currentSettings.copy(progressionType = prog)
                                onSettingsChanged(currentSettings)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Volt)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prog.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Volt
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = prog.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Приоритетные группы ──
            SectionTitle("Приоритетные группы мышц")
            SectionSubtitle("Получат дополнительное упражнение в плане")
            Spacer(modifier = Modifier.height(Spacing.sm))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                MuscleGroup.entries.forEach { group ->
                    val isSelected = group in currentSettings.priorityGroups
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val updated = if (isSelected) {
                                currentSettings.priorityGroups - group
                            } else {
                                currentSettings.priorityGroups + group
                            }
                            currentSettings = currentSettings.copy(priorityGroups = updated)
                            onSettingsChanged(currentSettings)
                        },
                        label = { Text(group.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Volt.copy(alpha = 0.15f),
                            selectedLabelColor = Volt
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Переключатели ──
            SectionTitle("Дополнительно")
            Spacer(modifier = Modifier.height(Spacing.sm))

            SettingSwitch(
                title = "Разминочные подходы",
                subtitle = "Показывать разминку перед рабочими подходами",
                checked = currentSettings.includeWarmup,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(includeWarmup = it)
                    onSettingsChanged(currentSettings)
                }
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            SettingSwitch(
                title = "Авто-deload",
                subtitle = "Облегчённая неделя каждые ${currentSettings.deloadIntervalWeeks} нед.",
                checked = currentSettings.autoDeload,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(autoDeload = it)
                    onSettingsChanged(currentSettings)
                }
            )

            if (currentSettings.autoDeload) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Интервал deload: ${currentSettings.deloadIntervalWeeks} нед.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = Spacing.md)
                )
                Slider(
                    value = currentSettings.deloadIntervalWeeks.toFloat(),
                    onValueChange = {
                        currentSettings = currentSettings.copy(deloadIntervalWeeks = it.toInt())
                    },
                    onValueChangeFinished = {
                        onSettingsChanged(currentSettings)
                    },
                    valueRange = 3f..8f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = Volt,
                        activeTrackColor = Volt
                    ),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Свой сплит ──
            if (currentSettings.splitType == SplitType.CUSTOM) {
                SectionTitle("Настройка дней")
                SectionSubtitle("Выберите группы мышц для каждого дня")
                Spacer(modifier = Modifier.height(Spacing.sm))

                (0 until currentSettings.daysPerWeek).forEach { dayIndex ->
                    val dayGroups = currentSettings.customSplitDays[dayIndex] ?: emptyList()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = CardShape
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = "День ${dayIndex + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Volt
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                MuscleGroup.entries.forEach { group ->
                                    val isSelected = group in dayGroups
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updatedDay = if (isSelected) {
                                                dayGroups - group
                                            } else {
                                                dayGroups + group
                                            }
                                            val updatedMap = currentSettings.customSplitDays.toMutableMap()
                                            updatedMap[dayIndex] = updatedDay
                                            currentSettings = currentSettings.copy(customSplitDays = updatedMap)
                                            onSettingsChanged(currentSettings)
                                        },
                                        label = {
                                            Text(
                                                group.displayName,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Volt.copy(alpha = 0.15f),
                                            selectedLabelColor = Volt
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xl))
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SectionSubtitle(text: String) {
    Spacer(modifier = Modifier.height(Spacing.xxs))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = Volt
                )
            )
        }
    }
}
