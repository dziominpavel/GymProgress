package com.example.gymprogress.data.backup

import com.example.gymprogress.data.Exercise
import com.example.gymprogress.data.FormatUtils
import com.example.gymprogress.data.WorkoutEntry

data class PlannedMergeEntry(
    val entry: WorkoutEntry,
    val exerciseClientId: String?
)

data class MergePlan(
    val newExercises: List<Exercise>,
    val updatedExercises: List<Exercise>,
    val newEntries: List<PlannedMergeEntry>,
    val matchedExercises: Int,
    val matchedEntries: Int
) {
    val addedExercises: Int get() = newExercises.size
    val addedEntries: Int get() = newEntries.size
}

object ImportMerger {

    fun planMerge(
        currentExercises: List<Exercise>,
        currentEntries: List<WorkoutEntry>,
        backup: BackupFileV1
    ): MergePlan {
        requireUniqueFileExercises(backup.exercises)

        val currentByClientId = currentExercises.associateBy { it.clientId }
        val currentByNameKey = currentExercises.associateBy { it.nameKey }
        val resolvedByFileClientId = mutableMapOf<String, Exercise>()
        val newExercises = mutableListOf<Exercise>()
        val updatedExercises = mutableListOf<Exercise>()
        var matchedExercises = 0

        backup.exercises.forEach { fileExercise ->
            val nameKey = FormatUtils.normalizeExerciseNameKey(fileExercise.name)
            val byClientId = currentByClientId[fileExercise.clientId]
            val byName = currentByNameKey[nameKey]

            if (byName != null && byClientId != null && byName.id != byClientId.id) {
                // Файл связывает clientId одного упражнения с именем другого.
                // Не переименовываем существующее, чтобы не нарушить уникальность.
                resolvedByFileClientId[fileExercise.clientId] = byName
                matchedExercises++
                return@forEach
            }

            val target = byClientId ?: byName
            if (target != null) {
                val updated = target.copy(
                    name = fileExercise.name,
                    muscleGroup = fileExercise.muscleGroup,
                    exerciseType = fileExercise.exerciseType,
                    isBodyweight = fileExercise.isBodyweight,
                    nameKey = nameKey,
                    isArchived = target.isArchived
                )
                updatedExercises += updated
                resolvedByFileClientId[fileExercise.clientId] = updated
                matchedExercises++
            } else {
                val created = Exercise(
                    name = fileExercise.name,
                    muscleGroup = fileExercise.muscleGroup,
                    exerciseType = fileExercise.exerciseType,
                    isBodyweight = fileExercise.isBodyweight,
                    isArchived = fileExercise.isArchived,
                    clientId = fileExercise.clientId,
                    nameKey = nameKey
                )
                newExercises += created
                resolvedByFileClientId[fileExercise.clientId] = created
            }
        }

        val currentEntryByClientId = currentEntries.associateBy { it.clientId }
        val naturalKeys = currentEntries.mapTo(mutableSetOf()) { entry ->
            naturalKey(
                exerciseId = entry.exerciseId,
                exerciseClientId = null,
                exerciseName = entry.exerciseName,
                date = entry.date,
                weight = entry.weight,
                reps = entry.reps
            )
        }
        val importedClientIds = mutableSetOf<String>()
        val newEntries = mutableListOf<PlannedMergeEntry>()
        var matchedEntries = 0

        backup.entries.forEach { fileEntry ->
            if (!importedClientIds.add(fileEntry.clientId)) {
                matchedEntries++
                return@forEach
            }
            if (currentEntryByClientId.containsKey(fileEntry.clientId)) {
                matchedEntries++
                return@forEach
            }

            val targetExercise = fileEntry.exerciseClientId?.let { resolvedByFileClientId[it] }
            val exerciseName = targetExercise?.name ?: fileEntry.exerciseName
            val key = naturalKey(
                exerciseId = targetExercise?.id?.takeIf { it > 0 },
                exerciseClientId = fileEntry.exerciseClientId,
                exerciseName = exerciseName,
                date = fileEntry.date,
                weight = fileEntry.weight,
                reps = fileEntry.reps
            )
            if (!naturalKeys.add(key)) {
                matchedEntries++
                return@forEach
            }

            newEntries += PlannedMergeEntry(
                entry = WorkoutEntry(
                    date = fileEntry.date,
                    exerciseName = exerciseName,
                    weight = fileEntry.weight,
                    reps = fileEntry.reps,
                    exerciseId = targetExercise?.id?.takeIf { it > 0 },
                    clientId = fileEntry.clientId
                ),
                exerciseClientId = fileEntry.exerciseClientId
            )
        }

        return MergePlan(
            newExercises = newExercises,
            updatedExercises = updatedExercises,
            newEntries = newEntries,
            matchedExercises = matchedExercises,
            matchedEntries = matchedEntries
        )
    }

    private fun requireUniqueFileExercises(exercises: List<BackupExerciseV1>) {
        val clientIds = mutableSetOf<String>()
        val nameKeys = mutableSetOf<String>()
        exercises.forEach { exercise ->
            require(clientIds.add(exercise.clientId)) {
                "В файле повторяется clientId упражнения: ${exercise.clientId}"
            }
            require(nameKeys.add(FormatUtils.normalizeExerciseNameKey(exercise.name))) {
                "В файле два упражнения с одинаковым именем: ${exercise.name}"
            }
        }
    }

    private fun naturalKey(
        exerciseId: Long?,
        exerciseClientId: String?,
        exerciseName: String,
        date: String,
        weight: Double,
        reps: String
    ): String {
        val exerciseKey = when {
            exerciseId != null && exerciseId > 0 -> "id:$exerciseId"
            !exerciseClientId.isNullOrBlank() -> "client:$exerciseClientId"
            else -> "name:${FormatUtils.normalizeExerciseNameKey(exerciseName)}"
        }
        return listOf(exerciseKey, date, weight.toString(), reps.trim()).joinToString("|")
    }
}
