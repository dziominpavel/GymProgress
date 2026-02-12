package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.CardShapeSmall
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.TextFieldShape
import com.example.gymprogress.ui.theme.Volt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    exerciseNames: List<String>,
    selectedExercise: String?,
    entriesForExercise: List<WorkoutEntry>,
    onExerciseSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = Spacing.md)
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "ПРОГРЕСС",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Volt)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Отслеживайте свои результаты",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (exerciseNames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = Spacing.xxl)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📊", style = MaterialTheme.typography.displaySmall)
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        "Добавьте записи в журнал,\nчтобы отслеживать прогресс",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedExercise ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Выберите упражнение") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = TextFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    exerciseNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onExerciseSelected(name)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedExercise != null && entriesForExercise.isNotEmpty()) {
                val maxWeight = entriesForExercise.maxOf { it.weight }
                val totalSets = entriesForExercise.sumOf { it.reps.split(",").size }
                val totalReps = entriesForExercise.sumOf { entry ->
                    entry.reps.split(",").sumOf { it.trim().toIntOrNull() ?: 0 }
                }
                val sessionsCount = entriesForExercise.map { it.date }.distinct().size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    StatCard(
                        emoji = "🏆",
                        title = "Макс. вес",
                        value = "$maxWeight кг",
                        isHighlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        emoji = "🔄",
                        title = "Тренировок",
                        value = "$sessionsCount",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        emoji = "💪",
                        title = "Повторов",
                        value = "$totalReps",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Volt)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "История",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(entriesForExercise) { entry ->
                        HistoryRow(entry = entry, maxWeight = maxWeight)
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.md)) }
                }
            } else if (selectedExercise != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Нет данных для этого упражнения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun StatCard(
    emoji: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Card(
        modifier = modifier
            .then(
                if (isHighlight) Modifier.border(
                    width = 1.5.dp,
                    color = Volt.copy(alpha = 0.4f),
                    shape = CardShape
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight)
                Volt.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) Volt
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: WorkoutEntry, maxWeight: Double) {
    val isMax = entry.weight == maxWeight

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShapeSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.date,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(86.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(
                        if (isMax) Volt.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (isMax) Modifier.border(
                            1.dp,
                            Volt.copy(alpha = 0.3f),
                            RoundedCornerShape(Spacing.xs)
                        ) else Modifier
                    )
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
            ) {
                Text(
                    text = "${entry.weight} кг",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMax) Volt
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            val repsList = entry.reps.split(",").map { it.trim() }
            Text(
                text = repsList.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
