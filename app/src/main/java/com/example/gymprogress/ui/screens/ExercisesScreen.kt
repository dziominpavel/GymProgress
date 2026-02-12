package com.example.gymprogress.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.MuscleGroup
import com.example.gymprogress.ui.components.MuscleGroupIcon
import com.example.gymprogress.ui.theme.CardShape
import com.example.gymprogress.ui.theme.CardShapeSmall
import com.example.gymprogress.ui.theme.FabShape
import com.example.gymprogress.ui.theme.Spacing
import com.example.gymprogress.ui.theme.Volt

@Composable
fun ExercisesScreen(
    exercises: List<Exercise>,
    onAddExercise: (name: String, muscleGroup: String) -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val grouped = exercises.groupBy { it.muscleGroup }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Volt,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = FabShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить упражнение")
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
                text = "УПРАЖНЕНИЯ",
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
                text = "${exercises.size} упражнений в ${grouped.size} группах",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            if (exercises.isEmpty()) {
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
                                "🏋️",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            "Упражнений пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            "Нажмите + чтобы создать первое",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    grouped.forEach { (group, groupExercises) ->
                        val displayName = MuscleGroup.entries
                            .find { it.name == group }?.displayName ?: group
                        val isExpanded = expandedGroups[group] != false

                        item(key = "header_$group") {
                            MuscleGroupHeader(
                                muscleGroup = group,
                                name = displayName,
                                count = groupExercises.size,
                                isExpanded = isExpanded,
                                onClick = { expandedGroups[group] = !isExpanded }
                            )
                        }

                        if (isExpanded) {
                            items(groupExercises, key = { it.id }) { exercise ->
                                ExerciseItem(
                                    exercise = exercise,
                                    onDelete = { exerciseToDelete = exercise }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, group ->
                onAddExercise(name, group)
                showAddDialog = false
            }
        )
    }

    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Удалить упражнение?") },
            text = { Text(exercise.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExercise(exercise)
                    exerciseToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun MuscleGroupHeader(
    muscleGroup: String,
    name: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.5.dp,
                        color = Volt.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                MuscleGroupIcon(
                    muscleGroup = muscleGroup,
                    size = 56.dp,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.xxs))
                            .background(Volt.copy(alpha = 0.15f))
                            .padding(horizontal = Spacing.xs, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$count упр.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Volt
                        )
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Volt
            )
        }
    }
}

@Composable
private fun ExerciseItem(exercise: Exercise, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = CardShapeSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Volt)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onDelete,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var groupError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Новое упражнение", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Название") },
                    singleLine = true,
                    isError = nameError,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Мышечная группа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        isError = groupError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MuscleGroup.entries.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.displayName) },
                                onClick = {
                                    selectedGroup = group
                                    groupError = false
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                nameError = name.isBlank()
                groupError = selectedGroup == null
                if (!nameError && !groupError) {
                    onConfirm(name, selectedGroup!!.name)
                }
            }) {
                Text("Создать", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
