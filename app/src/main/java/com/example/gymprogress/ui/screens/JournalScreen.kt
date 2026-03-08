package com.example.gymprogress.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<WorkoutEntry>,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    onAddClick: () -> Unit,
    onDeleteEntry: (WorkoutEntry) -> Unit,
    onUpdateEntry: (WorkoutEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var entryToEdit by remember { mutableStateOf<WorkoutEntry?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Volt,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = FabShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp
                )
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
            val todayLabel = remember {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru"))
                today.format(formatter)
            }
            if (entries.isEmpty()) {
                Text(
                    text = todayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "$todayLabel · ${entries.size} записей",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            if (entries.isEmpty()) {
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
                                "📝",
                                style = MaterialTheme.typography.displaySmall
                            )
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
                val sortedEntries = remember(entries) {
                    entries.sortedWith(
                        compareByDescending<WorkoutEntry> { FormatUtils.parseStorageDate(it.date) ?: LocalDate.MIN }
                            .thenBy { it.id }
                    )
                }

                val grouped = sortedEntries.groupBy { it.date }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    grouped.forEach { (date, dateEntries) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = Spacing.xs)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp, 24.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Volt)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = FormatUtils.formatDate(date),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Volt
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Spacing.xxs))
                                            .background(Volt.copy(alpha = 0.15f))
                                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${dateEntries.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Volt
                                        )
                                    }
                                }
                            }
                        }

                        items(dateEntries, key = { it.id }) { entry ->
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
            // Left accent bar
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
                // Weight badge
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
