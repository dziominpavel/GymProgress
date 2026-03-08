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
                    HelpHeader("📊 Итоговый балл")
                    Text("Сравнивает сегодняшнюю тренировку со средним уровнем за последние 5 сессий. 1.0 = обычная тренировка, >1.0 = лучше вашей нормы, <1.0 = ниже нормы. Прогресс в % показывает отклонение от вашей же базы, а не от рекорда.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpFormula("Балл = Интенсивность + Эфф.Объём + Качество\n       + Бонусы − Штрафы\n1.0 = обычная тренировка, >1.0 = лучше нормы")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 1 ---
                    HelpHeader("🏋️ Компонент 1: Интенсивность (вес штанги)")
                    HelpChip("Гипертрофия / Базовые: 45%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Насколько ваш рабочий вес отличается от среднего за последние 5 тренировок. 1.0 = обычный вес, >1.0 = тяжелее нормы.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Score  = вес_сегодня / средний_вес_за_5_сессий\nВклад  = Score × 0.45")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("Средний вес: 60 кг\n60 кг → 1.00 × 0.45 = 0.450 (норма)\n63 кг → 1.05 × 0.45 = 0.473 (+5% к базе)")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("Стабильный вес = 0% вклада от интенсивности. Прибавка веса сразу отражается в %. Снижение — показывает минус.")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 2 ---
                    HelpHeader("📊 Компонент 2: Эффективный объём")
                    HelpChip("Гипертрофия / Базовые: 35%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Тоннаж, где повторения весят по-разному в зависимости от попадания в целевой диапазон:",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("В целевом диапазоне (8–12)", "× 1.0  ✅")
                    HelpRow("Близко к диапазону (6–7 / 13–15)", "× 0.7  ⚠️")
                    HelpRow("Далеко от диапазона (≤5 / ≥16)", "× 0.3  ❌")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Эфф.объём = вес × Σ(повторы × коэфф.)\nВклад = (Эфф.объём / средний_за_5) × 0.35")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("Средний объём: 1800 кг\n1800 кг → 1.00 × 0.35 = 0.350 (норма)\n2160 кг → 1.20 × 0.35 = 0.420 (+20% объёма)")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpNote("Стабильный объём = 0% вклада. Один раз сделал меньше из-за усталости — база снизилась, поэтому следующая нормальная тренировка не будет ложно показывать большой рост.")
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Компонент 3 ---
                    HelpHeader("⭐ Компонент 3: Качество повторений")
                    HelpChip("Гипертрофия / Базовые: 20%")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("В целевом диапазоне", "1.0")
                    HelpRow("Близко к диапазону", "0.6")
                    HelpRow("За пределами диапазона", "0.2")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Score = качество_сегодня / среднее_качество_за_5\nВклад = Score × 0.20")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("Средн. качество: 0.8 (обычно попадаешь в диапазон)\nСегодня: 1.0 → 1.0/0.8 × 0.20 = 0.250 (+25%)\nСегодня: 0.8 → 1.0 × 0.20 = 0.200 (норма)")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Бонусы ---
                    HelpHeader("🎁 Бонусы")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("🏆 Новый рекорд веса (PR)", "+0.060")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Срабатывает только при строгом увеличении веса (больше предыдущего максимума). Следующая тренировка с тем же весом бонуса не получает и минуса тоже нет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Оптимальность подходов ---
                    HelpHeader("⚡ Оптимальность подходов")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Количество подходов влияет на оценку двусторонне: слишком мало = недостаточный стимул, слишком много = мусорный объём.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("1 подход", "−0.04")
                    HelpRow("2 подхода", "−0.01")
                    HelpRow("3–5 подходов (оптимум)", "+0.02")
                    HelpRow("6 подходов", "0.00")
                    HelpRow("7+ подходов (мусорный объём)", "−0.03")
                    Spacer(modifier = Modifier.height(4.dp))
                    HelpNote("Наука: гипертрофия — 3-5 рабочих подходов оптимально. Больше 6 = жунк объём, растёт усталость, снижается результат. Для силы оптимум 3-5, для выносливости 2-4.")
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Сэндбэгинг ---
                    HelpHeader("📈 Сэндбэгинг (восходящие повторения)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Если повторения растут от подхода к подходу (7,8,9) — это сэндбэгинг: первые подходы были слишком лёгкими (высокий RIR). В отличие от 9,8,7 — нормальный спад из-за усталости.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpFormula("Наклон = линейная регрессия повторений по номерам подходов")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Наклон > 2.0 (сильный рост)", "−0.08")
                    HelpRow("Наклон 1.0–2.0 (умеренный)", "−0.05")
                    HelpRow("Наклон 0.3–1.0 (лёгкий)", "−0.02")
                    HelpRow("Нисходящий или ровный (9,8,7)", "0.00")
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpExample("7,8,9 → наклон = +1.0 → штраф −0.05\n9,8,7 → наклон = −1.0 → штраф 0.00")
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Штраф за усталость ---
                    HelpHeader("⚠️ Штраф за усталость (резкий спад)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Анализирует разброс повторений и падение в последнем подходе. Резкое падение (10,6,4) = перегрузка.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    HelpRow("Разброс ≤ 10%", "0.00")
                    HelpRow("Разброс 10–20%", "−0.02")
                    HelpRow("Разброс 20–30%", "−0.05")
                    HelpRow("Разброс 30–40%", "−0.09")
                    HelpRow("Разброс > 40%", "−0.12")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Почему 0.992 ---
                    HelpHeader("❓ Почему балл 0.992, а не 1.0?")
                    Text("Теоретический потолок с PR и 5 подходами:",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    HelpFormula("0.450 + 0.350 + 0.200 + 0.060 + 0.050 = 1.110 → 1.000")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Балл 0.992 означает: один из компонентов не на максимуме. Чаще всего — эффективный объём (была сессия с большим числом подходов). Это не плохой результат — это честная оценка.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Таблица весов ---
                    HelpHeader("⚙️ Веса по цели тренировки")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Цель / Тип", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                        Text("💪", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("📊", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("⭐", style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    HelpGoalRow("Гипертрофия / Базовые", "45%", "35%", "20%", highlight = true)
                    HelpGoalRow("Гипертрофия / Изоляция", "30%", "35%", "35%")
                    HelpGoalRow("Сила / Базовые", "60%", "20%", "20%")
                    HelpGoalRow("Сила / Изоляция", "50%", "25%", "25%")
                    HelpGoalRow("Выносливость", "15%", "55%", "30%")
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Расшифровка чисел ---
                    HelpHeader("🔢 Что означают два числа в таблице результата")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text("🏋️ Интенсивность", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.8f))
                        Text("55→60 кг", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.6f))
                        Text("0.450", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        Text("+0.038", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                            modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpRow("Балл (0.450)", "Очки этого компонента сегодня")
                    HelpRow("Δ (+0.038)", "Изменение очков vs прошлая тренировка")
                    Spacer(modifier = Modifier.height(8.dp))
                    HelpNote("0.450 = подняли лучший вес (1.0 × 0.45). Если бы вес был 55 из 60 кг лучшего → 0.917 × 0.45 = 0.412. Разница 0.038 — это и есть прогресс в баллах.")
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

@Composable
internal fun HelpGoalRow(
    goal: String, wI: String, wEV: String, wR: String, highlight: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(goal, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2.2f))
        Text(wI, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Volt else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(wEV, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(wR, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
    }
}
