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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.WorkoutEntry
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    entries: List<WorkoutEntry>,
    onAddClick: () -> Unit,
    onDeleteEntry: (WorkoutEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var entryToDelete by remember { mutableStateOf<WorkoutEntry?>(null) }

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
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "${entries.size} записей",
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
                            "Записей пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            "Нажмите + чтобы добавить\nпервую тренировку",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val grouped = entries.groupBy { it.date }

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
                                        text = date,
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
                                onLongClick = { entryToDelete = entry },
                                onDeleteClick = { entryToDelete = entry }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Удалить запись?", fontWeight = FontWeight.Bold) },
            text = {
                Text("${entry.exerciseName} — ${entry.weight} кг (${entry.date})")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEntry(entry)
                    entryToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkoutEntryCard(
    entry: WorkoutEntry,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                            text = entry.weight.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            },
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        entry.reps.split(",").forEachIndexed { index, rep ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}: ${rep.trim()} повт.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
