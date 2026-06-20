package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.Gender
import com.example.gymprogress.data.ScoringSystem
import com.example.gymprogress.data.TrainingGoal
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentGoal: TrainingGoal,
    bodyWeightKg: Double?,
    currentScoringSystem: ScoringSystem,
    currentGender: Gender?,
    currentHeightCm: Int?,
    isAnthropometryComplete: Boolean,
    timerSoundEnabled: Boolean,
    timerVibrationEnabled: Boolean,
    membershipExpiryDate: LocalDate?,
    onGoalChanged: (TrainingGoal) -> Unit,
    onBodyWeightChanged: (Double?) -> Unit,
    onScoringSystemChanged: (ScoringSystem) -> Unit,
    onGenderChanged: (Gender?) -> Unit,
    onHeightCmChanged: (Int?) -> Unit,
    onTimerSoundEnabledChanged: (Boolean) -> Unit,
    onTimerVibrationEnabledChanged: (Boolean) -> Unit,
    onMembershipExpiryDateChanged: (LocalDate?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bodyWeightText by remember(bodyWeightKg) {
        mutableStateOf(bodyWeightKg?.let { FormatUtils.formatWeight(it) } ?: "")
    }
    var bodyWeightError by remember { mutableStateOf(false) }
    var heightText by remember(currentHeightCm) {
        mutableStateOf(currentHeightCm?.toString() ?: "")
    }
    var heightError by remember { mutableStateOf(false) }
    var showMembershipDatePicker by remember { mutableStateOf(false) }
    fun saveAll(): Boolean {
        var ok = true
        val bwValue = bodyWeightText.replace(",", ".").toDoubleOrNull()
        if (bodyWeightText.isBlank()) {
            onBodyWeightChanged(null)
            bodyWeightError = false
        } else if (bwValue != null && bwValue > 0) {
            onBodyWeightChanged(bwValue)
            bodyWeightError = false
        } else {
            bodyWeightError = true
            ok = false
        }
        val hValue = heightText.toIntOrNull()
        if (heightText.isBlank()) {
            onHeightCmChanged(null)
            heightError = false
        } else if (hValue != null && hValue in 50..300) {
            onHeightCmChanged(hValue)
            heightError = false
        } else {
            heightError = true
            ok = false
        }
        return ok
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (saveAll()) {
                            onBack()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "НАСТРОЙКИ",
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

            // ── Система оценки прогресса ──
            Text(
                text = "Система оценки прогресса",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Определяет, как рассчитывается прогресс по упражнениям",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            ScoringSystem.entries.forEach { system ->
                val isSelected = system == currentScoringSystem
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CardShape)
                        .clickable { onScoringSystemChanged(system) }
                        .then(
                            if (isSelected) Modifier.border(
                                1.5.dp,
                                Volt.copy(alpha = 0.5f),
                                CardShape
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
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onScoringSystemChanged(system) },
                            colors = RadioButtonDefaults.colors(selectedColor = Volt)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = system.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Volt
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = system.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (currentScoringSystem == ScoringSystem.SIMPLIFIED && !isAnthropometryComplete) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = CardShape
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Укажите вес тела",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = "Для корректной работы упрощённой системы необходимо указать вес тела. Без него оценка для упражнений с собственным весом будет недоступна.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Антропометрия ──
            Text(
                text = "Параметры тела",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Нужны для упражнений с собственным весом и точности оценки",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = bodyWeightText,
                onValueChange = {
                    bodyWeightText = it
                    bodyWeightError = false
                },
                label = { Text("Вес тела (кг)") },
                singleLine = true,
                isError = bodyWeightError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = heightText,
                onValueChange = {
                    heightText = it
                    heightError = false
                },
                label = { Text("Рост (см)") },
                singleLine = true,
                isError = heightError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Пол",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Row {
                Gender.entries.forEach { g ->
                    val isSelected = g == currentGender
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onGenderChanged(g) }
                            .padding(end = Spacing.md)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onGenderChanged(g) },
                            colors = RadioButtonDefaults.colors(selectedColor = Volt)
                        )
                        Text(
                            text = g.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Volt else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            TextButton(onClick = { saveAll() }) {
                Text("Сохранить параметры", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "Цель тренировок",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Влияет на формулу оценки прогресса и целевой диапазон повторений",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            TrainingGoal.entries.forEach { goal ->
                val isSelected = goal == currentGoal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CardShape)
                        .clickable { onGoalChanged(goal) }
                        .then(
                            if (isSelected) Modifier.border(
                                1.5.dp,
                                Volt.copy(alpha = 0.5f),
                                CardShape
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
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onGoalChanged(goal) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Volt
                            )
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Volt
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = goal.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Активная тренировка ──
            Text(
                text = "Активная тренировка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Сигнал на финале таймера отдыха",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            TimerSettingSwitch(
                title = "Звук таймера",
                subtitle = "Короткие бипы за 3·2·1 сек и длинный по нулю",
                checked = timerSoundEnabled,
                onCheckedChange = onTimerSoundEnabledChanged
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            TimerSettingSwitch(
                title = "Вибрация таймера",
                subtitle = "Короткая вибрация на финале отдыха",
                checked = timerVibrationEnabled,
                onCheckedChange = onTimerVibrationEnabledChanged
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Абонемент ──
            Text(
                text = "Дата окончания абонемента",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "За 7 дней до окончания придёт напоминание о продлении",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .clickable { showMembershipDatePicker = true },
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
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = Volt
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (membershipExpiryDate != null) {
                                FormatUtils.formatDate(membershipExpiryDate.toString())
                            } else {
                                "Не указано"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (membershipExpiryDate != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (membershipExpiryDate == null) {
                            Text(
                                text = "Нажмите, чтобы выбрать дату",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (membershipExpiryDate != null) {
                        IconButton(onClick = { onMembershipExpiryDateChanged(null) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Очистить дату",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = CardShape
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Как это работает",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = if (currentScoringSystem == ScoringSystem.SIMPLIFIED) {
                            "Упрощённая система:\n" +
                            "• Оценочный 1RM (кг) — максимальный вес на 1 повторение\n" +
                            "• Гибридная формула (Epley до 10 повторов, Brzycki выше)\n" +
                            "• Бонус за подтверждающие подходы (до +5%)\n" +
                            "• Прогресс = рост/падение 1RM во времени"
                        } else {
                            "Усложнённая система:\n" +
                            "• Гипертрофия — составной стимул (напряжение + продуктивность + качество)\n" +
                            "• Сила — акцент на рабочий вес (3–6 повторов)\n" +
                            "• Выносливость — акцент на объём и количество повторов (15–25)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }

    if (showMembershipDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (membershipExpiryDate ?: LocalDate.now(ZoneId.systemDefault()))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Сравниваем по дням в системном TZ, чтобы запретить прошедшие даты.
                    val selectedDay = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val today = LocalDate.now(ZoneId.systemDefault())
                    return !selectedDay.isBefore(today)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showMembershipDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onMembershipExpiryDateChanged(selected)
                    }
                    showMembershipDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMembershipDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimerSettingSwitch(
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
