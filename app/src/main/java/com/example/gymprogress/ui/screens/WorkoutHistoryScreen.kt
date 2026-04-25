package com.example.gymprogress.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.components.EmptyState
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private fun whParseDate(s: String): LocalDate = FormatUtils.parseStorageDate(s) ?: LocalDate.MIN

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutHistoryScreen(
    entries: List<WorkoutEntry>,
    exercises: List<Exercise>,
    bodyWeightKg: Double?,
    personalRecordEntryIds: Set<Long>,
    onDeleteEntry: (WorkoutEntry) -> Unit,
    onUpdateEntry: (WorkoutEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var entryToEdit by remember { mutableStateOf<WorkoutEntry?>(null) }

    // Soft-delete с Undo-Snackbar (аналогично JournalScreen): запись прячется из UI,
    // реальное удаление происходит только если пользователь не нажал «Отменить».
    val pendingDelete = remember { mutableStateMapOf<Long, WorkoutEntry>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun requestDeleteWithUndo(entry: WorkoutEntry) {
        pendingDelete[entry.id] = entry
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Запись удалена",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> pendingDelete.remove(entry.id)
                SnackbarResult.Dismissed -> {
                    if (pendingDelete.remove(entry.id) != null) {
                        onDeleteEntry(entry)
                    }
                }
            }
        }
    }

    val workoutDates = remember(entries) {
        entries.map { whParseDate(it.date) }
            .filter { it != LocalDate.MIN }
            .toSet()
    }

    // Список: сверху первые по дате (старые), ниже — следующие (консистентно по проекту)
    val visibleEntries = remember(entries, pendingDelete.toMap()) {
        entries.filter { it.id !in pendingDelete }
    }
    val filteredEntries = remember(visibleEntries, selectedDate) {
        if (selectedDate == null) {
            visibleEntries.sortedWith(compareBy({ whParseDate(it.date) }, { it.id }))
        } else {
            visibleEntries.filter { whParseDate(it.date) == selectedDate }.sortedBy { it.id }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = Volt
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "ИСТОРИЯ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
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
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            WorkoutCalendar(
                month = displayedMonth,
                workoutDates = workoutDates,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = if (selectedDate == date) null else date
                },
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                modifier = Modifier.padding(horizontal = Spacing.md)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedDate != null) {
                    Text(
                        text = FormatUtils.formatDate(FormatUtils.toStorageDate(selectedDate!!)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { selectedDate = null }) {
                        Text("Все даты", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        text = "Все тренировки",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${filteredEntries.size} записей",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Группы по дате: сверху старые даты, внутри даты — записи по id (сначала введённые)
            val grouped = remember(filteredEntries) {
                filteredEntries
                    .groupBy { it.date }
                    .entries
                    .sortedBy { (date, _) -> whParseDate(date) }
                    .associate { it.key to it.value.sortedBy { e -> e.id } }
            }

            if (filteredEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = if (selectedDate != null) "В этот день тренировок не было"
                            else "Нет записей тренировок",
                    description = if (selectedDate != null)
                            "Выберите другой день в календаре или сбросьте фильтр"
                        else
                            "Добавьте первую тренировку в журнале",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.md)
                )
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.md),
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
                                isPersonalRecord = entry.id in personalRecordEntryIds,
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
                            onClick = { selectedEntry = null; entryToEdit = entry },
                            modifier = Modifier.weight(1f)
                        ) { Text("Редактировать") }
                        TextButton(
                            onClick = { requestDeleteWithUndo(entry); selectedEntry = null },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) { Text("Удалить") }
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
            onConfirm = { updated -> onUpdateEntry(updated); entryToEdit = null }
        )
    }
}
