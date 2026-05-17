package com.example.gymprogress.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.ExerciseRecommendation
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.SetType
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt

@Composable
fun TrainerScreen(
    recommendation: WorkoutRecommendation?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (WorkoutRecommendation) -> Unit,
    isAiAvailable: Boolean = false,
    aiAdvice: String? = null,
    aiLoading: Boolean = false,
    onAskAi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (recommendation != null && recommendation.exercises.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onStartWorkout(recommendation) },
                    containerColor = Volt,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = FabShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Начать тренировку")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md)
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
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
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки тренера",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt)
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            when {
                recommendation == null -> {
                    EmptyTrainerState(missingGroups = emptyList())
                }
                recommendation.exercises.isEmpty() -> {
                    EmptyTrainerState(missingGroups = recommendation.missingGroups)
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        item {
                            NextWorkoutHeader(recommendation)
                        }

                        if (recommendation.missingGroups.isNotEmpty()) {
                            item {
                                MissingGroupsHint(recommendation.missingGroups)
                            }
                        }

                        items(recommendation.exercises) { exerciseRec ->
                            ExerciseRecCard(exerciseRec)
                        }

                        if (isAiAvailable) {
                            item {
                                AiAdviceSection(
                                    advice = aiAdvice,
                                    isLoading = aiLoading,
                                    onAskAi = onAskAi
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTrainerState(missingGroups: List<MuscleGroup>) {
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🤖",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
            if (missingGroups.isNotEmpty()) {
                Text(
                    "Не хватает упражнений",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    "Добавьте упражнения для групп: ${missingGroups.joinToString(", ") { it.displayName }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    "Вкладка «Упражнения» → нажмите +",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Volt,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Нет рекомендаций",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    "Добавьте упражнения на вкладке «Упражнения», и тренер составит для вас план",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MissingGroupsHint(missingGroups: List<MuscleGroup>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Нет упражнений для: ${missingGroups.joinToString(", ") { it.displayName }}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "Добавьте их на вкладке «Упражнения», чтобы получить полный план",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NextWorkoutHeader(recommendation: WorkoutRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Volt.copy(alpha = 0.1f)
        ),
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Следующая тренировка",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Volt
                )
                if (recommendation.isDeloadWeek) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                    ) {
                        Text(
                            "DELOAD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = recommendation.dayLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                recommendation.muscleGroups.forEach { group ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                    ) {
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            val workingSets = recommendation.exercises.sumOf { rec ->
                rec.sets.count { it.type == SetType.WORKING }
            }
            Text(
                text = "${recommendation.exercises.size} упражнений · $workingSets рабочих подходов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExerciseRecCard(rec: ExerciseRecommendation) {
    val groupName = MuscleGroup.entries
        .find { it.name == rec.exercise.muscleGroup }?.displayName
        ?: rec.exercise.muscleGroup

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Volt)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = rec.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "🏆 Лучшая тренировка",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Volt
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = rec.bestEntry?.let { formatEntrySummary(it) } ?: "Нет данных",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Прошлая тренировка",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = rec.lastEntry?.let { formatEntrySummary(it) } ?: "Нет данных",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Следующая тренировка",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = formatRecommendationSummary(rec),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (rec.note != null || rec.advice != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(Spacing.sm)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "💡 Совет тренера",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Volt
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        
                        rec.note?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (rec.note != null && rec.advice != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        rec.advice?.let { advice ->
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatEntrySummary(entry: WorkoutEntry): String {
    val reps = entry.reps.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val repsLabel = if (reps.isNotEmpty()) " • ${reps.joinToString(" · ")}" else ""
    return "${FormatUtils.formatWeight(entry.weight)} кг${repsLabel} • ${FormatUtils.formatDate(entry.date)}"
}

private fun formatRecommendationSummary(rec: ExerciseRecommendation): String {
    val workingSets = rec.sets.filter { it.type == SetType.WORKING }
    val template = workingSets.firstOrNull()
    val reps = template?.targetReps?.let { "${it.first}–${it.last} повт." } ?: "повторы подобрать"
    val weight = template?.weight?.let { "${FormatUtils.formatWeight(it)} кг" } ?: "вес подобрать"
    return "${workingSets.size}× • $weight • $reps"
}

@Composable
private fun AiAdviceSection(
    advice: String?,
    isLoading: Boolean,
    onAskAi: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (advice == null && !isLoading) {
            OutlinedButton(
                onClick = onAskAi,
                border = BorderStroke(1.dp, Volt.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Volt)
            ) {
                Text("Спросить ИИ \uD83E\uDD16", fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Volt
                )
                Text(
                    "ИИ анализирует...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = advice != null) {
            advice?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    colors = CardDefaults.cardColors(
                        containerColor = Volt.copy(alpha = 0.08f)
                    ),
                    shape = CardShape
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Совет ИИ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Volt
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

