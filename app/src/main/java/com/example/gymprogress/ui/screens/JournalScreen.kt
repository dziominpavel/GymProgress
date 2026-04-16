package com.example.gymprogress.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.data.WorkoutRecommendation
import com.example.gymprogress.ui.components.MuscleGroupIcon
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<WorkoutEntry>,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    previousSession: List<WorkoutEntry>,
    previousSessionDate: String?,
    previousSessionTitleOverride: String?,
    previousSessionDayMuscleGroups: List<String>,
    splitDayOptions: List<Pair<Int, String>>,
    selectedDayIndex: Int?,
    onSelectDay: (Int?) -> Unit,
    workoutRecommendation: WorkoutRecommendation?,
    onAddClick: () -> Unit,
    onQuickAdd: (exerciseName: String) -> Unit,
    onOpenTrainer: () -> Unit,
    onDeleteEntry: (WorkoutEntry) -> Unit,
    onUpdateEntry: (WorkoutEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var entryToEdit by remember { mutableStateOf<WorkoutEntry?>(null) }

    val todayLabel = remember {
        FormatUtils.formatJournalDayMonth(LocalDate.now())
    }

    val previousSessionDayLabel = remember(previousSessionDate, previousSessionTitleOverride) {
        if (previousSessionTitleOverride != null) return@remember previousSessionTitleOverride
        if (previousSessionDate == null) return@remember null
        val parsed = FormatUtils.parseStorageDate(previousSessionDate) ?: return@remember null
        val today = LocalDate.now()
        val formattedDate = FormatUtils.formatDate(previousSessionDate)
        if (parsed.dayOfWeek == today.dayOfWeek) {
            val dayName = FormatUtils.formatJournalWeekday(parsed)
                .replaceFirstChar { it.uppercase() }
            "Прошлый $dayName · $formattedDate"
        } else {
            "Прошлая тренировка · $formattedDate"
        }
    }

    val previousExercises = remember(previousSession) {
        previousSession
            .groupBy { it.exerciseName }
            .map { (_, list) -> list.maxByOrNull { it.id } ?: list.first() }
            .sortedBy { it.id }
    }

    val hasAnyContent = workoutRecommendation != null || previousExercises.isNotEmpty() || entries.isNotEmpty() || previousSessionDayLabel != null

    val sortedTodayEntries = remember(entries) {
        entries.sortedWith(
            compareBy<WorkoutEntry> {
                FormatUtils.parseStorageDate(it.date) ?: LocalDate.MIN
            }.thenBy { it.id }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Volt,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = FabShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить запись")
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
            Text(
                text = "ЖУРНАЛ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
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
                text = if (entries.isEmpty()) todayLabel else "$todayLabel · ${entries.size} записей",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            if (!hasAnyContent) {
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
                            Text("📝", style = MaterialTheme.typography.displaySmall)
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            "Сегодня ещё нет тренировок",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            "Нажмите + чтобы добавить\nтренировку за сегодня",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Section 1: Trainer card (recommendation; при выборе дня — данные выбранного дня)
                    if (workoutRecommendation != null) {
                        item(key = "trainer_card") {
                            TrainerRecommendationCard(
                                recommendation = workoutRecommendation,
                                displayedDayLabel = selectedDayIndex?.let { idx ->
                                    splitDayOptions.find { it.first == idx }?.second
                                },
                                displayedDayMuscleGroups = previousSessionDayMuscleGroups,
                                displayedExerciseCount = if (selectedDayIndex != null) previousSession.size else null,
                                onOpenTrainer = onOpenTrainer
                            )
                        }
                    }

                    // Section 2: Previous session (by split or fallback)
                    if (previousSessionDayLabel != null) {
                        stickyHeader(key = "prev_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                if (splitDayOptions.isNotEmpty()) {
                                    PreviousSessionDaySelector(
                                        splitDayOptions = splitDayOptions,
                                        selectedDayIndex = selectedDayIndex,
                                        onSelectDay = onSelectDay
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.xxs))
                                }
                                SectionHeader(
                                    title = previousSessionDayLabel,
                                    count = previousExercises.size,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (previousSessionDayMuscleGroups.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = previousSessionDayMuscleGroups.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = Spacing.md)
                                    )
                                }
                            }
                        }
                        if (previousExercises.isNotEmpty()) {
                            items(previousExercises, key = { "prev_${it.exerciseName}" }) { entry ->
                                val exercise = exercises.find { it.name == entry.exerciseName }
                                PreviousSessionExerciseRow(
                                    entry = entry,
                                    exercise = exercise,
                                    onQuickAdd = onQuickAdd
                                )
                            }
                        }
                    }

                    // Section 3: Today
                    stickyHeader(key = "today_header") {
                        SectionHeader(
                            title = "Сегодня",
                            count = entries.size,
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        )
                    }

                    if (entries.isEmpty()) {
                        item(key = "today_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (previousExercises.isNotEmpty())
                                        "Нажмите «+» рядом с упражнением или добавьте новое"
                                    else
                                        "Нажмите + чтобы добавить первое упражнение",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = Spacing.lg)
                                )
                            }
                        }
                    } else {
                        items(sortedTodayEntries, key = { it.id }) { entry ->
                            WorkoutEntryCard(
                                entry = entry,
                                onLongClick = { selectedEntry = entry }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        Dialog(onDismissRequest = { selectedEntry = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        entry.exerciseName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${FormatUtils.formatWeight(entry.weight)} кг — ${entry.reps.split(",").size} подходов (${FormatUtils.formatDate(entry.date)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedEntry = null
                                entryToEdit = entry
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Редактировать")
                        }
                        TextButton(
                            onClick = {
                                onDeleteEntry(entry)
                                selectedEntry = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }

    entryToEdit?.let { entry ->
        EditEntryDialog(
            entry = entry,
            exercises = exercises,
            bodyWeightKg = bodyWeightKg,
            onDismiss = { entryToEdit = null },
            onConfirm = { updated ->
                onUpdateEntry(updated)
                entryToEdit = null
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Volt)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = Volt
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.xxs))
                    .background(Volt.copy(alpha = 0.15f))
                    .padding(horizontal = Spacing.xs, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Volt
                )
            }
        }
    }
}

@Composable
private fun TrainerRecommendationCard(
    recommendation: WorkoutRecommendation?,
    displayedDayLabel: String?,
    displayedDayMuscleGroups: List<String>,
    displayedExerciseCount: Int?,
    onOpenTrainer: () -> Unit
) {
    val dayLabel = displayedDayLabel ?: recommendation?.dayLabel ?: ""
    val muscleGroupsText = if (displayedDayMuscleGroups.isNotEmpty()) {
        displayedDayMuscleGroups.joinToString(" · ")
    } else {
        recommendation?.muscleGroups
            ?.mapNotNull { group -> MuscleGroup.entries.find { it.name == group.name }?.displayName }
            ?.joinToString(" · ")
            ?: ""
    }
    val exerciseCount = displayedExerciseCount ?: recommendation?.exercises?.size ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
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
            Box(
                modifier = Modifier
                    .size(4.dp, 56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Volt)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Тренер · $dayLabel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Volt
                )
                if (muscleGroupsText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = muscleGroupsText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$exerciseCount упражнений",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recommendation?.isDeloadWeek == true) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Неделя разгрузки",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            TextButton(onClick = onOpenTrainer) {
                Text("Подробнее", color = Volt)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviousSessionDaySelector(
    splitDayOptions: List<Pair<Int, String>>,
    selectedDayIndex: Int?,
    onSelectDay: (Int?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        FilterChip(
            selected = selectedDayIndex == null,
            onClick = { onSelectDay(null) },
            label = { Text("Авто") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Volt.copy(alpha = 0.2f),
                selectedLabelColor = Volt
            )
        )
        splitDayOptions.forEach { (dayIndex, label) ->
            FilterChip(
                selected = selectedDayIndex == dayIndex,
                onClick = { onSelectDay(dayIndex) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Volt.copy(alpha = 0.2f),
                    selectedLabelColor = Volt
                )
            )
        }
    }
}

@Composable
private fun PreviousSessionExerciseRow(
    entry: WorkoutEntry,
    exercise: Exercise?,
    onQuickAdd: (String) -> Unit
) {
    val setsCount = remember(entry.reps) {
        entry.reps.split(",").filter { it.isNotBlank() }.size
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MuscleGroupIcon(
                muscleGroup = exercise?.muscleGroup ?: "",
                size = 40.dp,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${FormatUtils.formatWeight(entry.weight)} кг · $setsCount подх.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onQuickAdd(entry.exerciseName) }) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Volt.copy(alpha = 0.15f))
                        .border(1.dp, Volt.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить ${entry.exerciseName}",
                        tint = Volt,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun WorkoutEntryCard(
    entry: WorkoutEntry,
    onLongClick: () -> Unit
) {
    val accentColor = Volt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(CardShape)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(
                            width = 1.5.dp,
                            color = accentColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = FormatUtils.formatWeight(entry.weight),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        Text(
                            text = "кг",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    val reps = remember(entry.reps) {
                        entry.reps.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reps.forEachIndexed { index, rep ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}: ${rep} повт.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
