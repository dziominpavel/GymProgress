package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.ui.theme.Volt

@Composable
internal fun ScoreFormulaHelpDialog(onDismiss: () -> Unit) {
    val scroll = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxHeight(0.92f)
        ) {
            Column {
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Volt,
                            modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Как считается оценка",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Разбор формулы прогресса по деталям",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // --- Итоговый балл ---
                    HelpHeader("📊 Балл: 100 = ваш рекорд")
                    Text("100 баллов ≈ все компоненты на уровне вашего лучшего. Первая тренировка = 100. Побили рекорд — получите 105, 112 и т.д. Прогресс в % — сравнение со средним за 3 сессии.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpFormula("Гипертрофия: Стимул = напряжение × W1 + продуктивность × W2 + качество × W3\nСила / Выносливость: метрика / ваш_рекорд × 100")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("95 = чуть не дотянул до своего лучшего. 100 = рекорд. 112 = побил рекорд на 12%.")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Гипертрофия v2.5 ---
                    HelpHeader("💪 Гипертрофия: 3 компонента стимула")
                    Text("Для гипертрофии балл складывается из трёх факторов, а не только из объёма. Тяжёлая работа в рабочем диапазоне не будет несправедливо снижена.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Напряжение", "Вес / лучший вес в истории")
                    HelpRow("Продуктивность", "√(стимул-единицы / лучшие)")
                    HelpRow("Качество", "Среднее попадание в продуктивную зону")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Базовые: 55% напряжение + 25% продуктивность + 20% качество\nИзолирующие: 30% напряжение + 45% продуктивность + 25% качество")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("Увеличили вес и все повторы в зоне? Это прогресс, даже если тоннаж чуть ниже.")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Продуктивная зона ---
                    HelpHeader("🎯 Продуктивная зона повторений")
                    Text("Для гипертрофии «хороший» диапазон шире, чем классические 8–12:",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Базовые: 5–10", "идеально (1.0)")
                    HelpRow("Базовые: 11–15", "отлично (0.95)")
                    HelpRow("Базовые: 3–4", "допустимо (0.75)")
                    HelpRow("Изолирующие: 8–15", "идеально (1.0)")
                    HelpRow("Изолирующие: 16–20", "отлично (0.95)")
                    Spacer(modifier = Modifier.height(6.dp))

                    HelpHeader("🎯 Сила и выносливость")
                    HelpRow("Сила", "E1RM = оценочный 1RM по лучшему подходу (Epley)")
                    HelpRow("Выносливость", "Объём = вес × сумма повторений")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Сравнение ---
                    HelpHeader("📈 Сравнение и прогресс")
                    Text("Текущая сессия сравнивается со средним за последние 3 сессии (не с одной прошлой).",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Прогресс % = (стимул_сегодня − среднее_за_3) / среднее_за_3 × 100")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("▲ Лучше", "≥ +5%")
                    HelpRow("→ Без изменений", "от −5% до +5%")
                    HelpRow("▼ Хуже", "≤ −5%")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("Сравнение с 3 сессиями сглаживает случайные провалы и всплески.")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Защита от ложного минуса ---
                    HelpHeader("🛡️ Защита: тяжёлая работа в зоне")
                    Text("Если вы увеличили вес ≥5%, все подходы в продуктивной зоне, а объём не обвалился — алгоритм не поставит ▼ Хуже.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("Присед: 60 кг × 10,12,11,12 → 70 кг × 8,8,8,9\nСтарая оценка: −8%. Новая: +5% (напряжение выросло)")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Штраф за усталость ---
                    HelpHeader("⚠️ Штраф за усталость")
                    Text("Резкое падение повторений от первого к последнему подходу (10,6,4) снижает балл.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("dropRate = 1 − (последний_подход / первый_подход)")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Падение ≤ 20%", "0.00")
                    HelpRow("Падение 20–35%", "−0.03")
                    HelpRow("Падение 35–50%", "−0.06")
                    HelpRow("Падение > 50%", "−0.10")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Что отображается ---
                    HelpHeader("🔢 Что означают числа")
                    HelpRow("Стимул (100 = рекорд)", "Гипертрофия: составной балл сессии")
                    HelpRow("Напряжение", "% от вашего лучшего веса")
                    HelpRow("Продуктивность", "√(стимул-единицы / лучшие)")
                    HelpRow("Δ (+5.7%)", "Прогресс vs среднее за 3 сессии")
                    HelpRow("Причина", "Подсказка (Напряжение ↑, Качество ↓ и т.д.)")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Понятно", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HelpHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black, color = Volt)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
internal fun HelpFormula(text: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(Volt.copy(alpha = 0.10f)).padding(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun HelpExample(text: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
        Text("Пример:\n$text", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun HelpNote(text: String) {
    Text("💡 $text", style = MaterialTheme.typography.bodySmall,
        color = Volt.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
}

@Composable
internal fun HelpChip(text: String) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(Volt.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = Volt)
    }
}

@Composable
internal fun HelpRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

