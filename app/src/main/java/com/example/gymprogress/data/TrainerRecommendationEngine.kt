package com.example.gymprogress.data

import kotlin.math.roundToLong
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TrainerRecommendationEngine {

    fun generateRecommendation(
        settings: TrainerSettings,
        trainingGoal: TrainingGoal,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>
    ): WorkoutRecommendation {
        val preferredDayIndex = determineNextDayIndex(settings, history, exercises)
        val totalDays = getTotalDays(settings)
        val isDeload = shouldDeload(settings, history)

        var bestDayIndex = preferredDayIndex
        var bestExerciseRecs: List<ExerciseRecommendation> = emptyList()

        for (offset in 0 until totalDays) {
            val dayIndex = (preferredDayIndex + offset) % totalDays
            val muscleGroups = getMuscleGroupsForDay(settings, dayIndex)
            val recs = buildExerciseList(
                muscleGroups = muscleGroups,
                priorityGroups = settings.priorityGroups,
                exercises = exercises,
                history = history,
                trainingGoal = trainingGoal,
                progressionType = settings.progressionType,
                includeWarmup = settings.includeWarmup,
                isDeload = isDeload
            )
            if (recs.isNotEmpty()) {
                bestDayIndex = dayIndex
                bestExerciseRecs = recs
                break
            }
        }

        val muscleGroups = getMuscleGroupsForDay(settings, bestDayIndex)
        val dayLabel = getDayLabel(settings, bestDayIndex)

        val coveredGroups = bestExerciseRecs.map { it.exercise.muscleGroup }.toSet()
        val missingGroups = muscleGroups.filter { it.name !in coveredGroups }

        return WorkoutRecommendation(
            dayLabel = dayLabel,
            dayIndex = bestDayIndex,
            muscleGroups = muscleGroups,
            exercises = bestExerciseRecs,
            isDeloadWeek = isDeload,
            missingGroups = missingGroups
        )
    }

    fun getAlternatives(
        exercise: Exercise,
        allExercises: List<Exercise>
    ): List<Exercise> {
        return allExercises.filter {
            it.id != exercise.id &&
            it.muscleGroup == exercise.muscleGroup &&
            it.exerciseType == exercise.exerciseType
        }
    }

    private fun getTotalDays(settings: TrainerSettings): Int {
        return when (settings.splitType) {
            SplitType.FULL_BODY -> 1
            SplitType.UPPER_LOWER -> 2
            SplitType.PUSH_PULL_LEGS -> 3
            SplitType.CUSTOM -> settings.customSplitDays.size.coerceAtLeast(1)
        }
    }

    private fun determineNextDayIndex(
        settings: TrainerSettings,
        history: List<WorkoutEntry>,
        exercises: List<Exercise> = emptyList()
    ): Int {
        if (history.isEmpty()) return 0

        val totalDays = getTotalDays(settings)
        if (totalDays == 1) return 0

        val lastDate = history.maxOfOrNull { parseDate(it.date) } ?: return 0
        val lastExerciseNames = history
            .filter { it.date == lastDate.format(DATE_FORMATTER) }
            .map { it.exerciseName }
            .toSet()

        val lastDayIndex = guessDayIndexFromExercises(settings, lastExerciseNames, exercises)
        return (lastDayIndex + 1) % totalDays
    }

    private fun guessDayIndexFromExercises(
        settings: TrainerSettings,
        exerciseNames: Set<String>,
        exercises: List<Exercise> = emptyList()
    ): Int {
        if (exerciseNames.isEmpty()) return 0

        val usedGroups = exercises
            .filter { it.name in exerciseNames }
            .map { it.muscleGroup }
            .toSet()

        if (usedGroups.isEmpty()) return 0

        val totalDays = getTotalDays(settings)

        var bestDay = 0
        var bestOverlap = 0

        for (dayIndex in 0 until totalDays) {
            val dayGroups = getMuscleGroupsForDay(settings, dayIndex).map { it.name }.toSet()
            val overlap = usedGroups.intersect(dayGroups).size
            if (overlap > bestOverlap) {
                bestOverlap = overlap
                bestDay = dayIndex
            }
        }

        return bestDay
    }

    private fun getMuscleGroupsForDay(
        settings: TrainerSettings,
        dayIndex: Int
    ): List<MuscleGroup> {
        return when (settings.splitType) {
            SplitType.FULL_BODY -> MuscleGroup.entries.toList()
            SplitType.UPPER_LOWER -> {
                if (dayIndex % 2 == 0) {
                    listOf(
                        MuscleGroup.CHEST, MuscleGroup.BACK,
                        MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS
                    )
                } else {
                    listOf(MuscleGroup.LEGS, MuscleGroup.ABS)
                }
            }
            SplitType.PUSH_PULL_LEGS -> {
                when (dayIndex % 3) {
                    0 -> listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
                    1 -> listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
                    else -> listOf(MuscleGroup.LEGS, MuscleGroup.ABS)
                }
            }
            SplitType.CUSTOM -> {
                settings.customSplitDays[dayIndex]
                    ?: MuscleGroup.entries.toList()
            }
        }
    }

    private fun getDayLabel(settings: TrainerSettings, dayIndex: Int): String {
        return when (settings.splitType) {
            SplitType.FULL_BODY -> "Full Body"
            SplitType.UPPER_LOWER -> if (dayIndex % 2 == 0) "Upper (Верх)" else "Lower (Низ)"
            SplitType.PUSH_PULL_LEGS -> when (dayIndex % 3) {
                0 -> "Push (Жим)"
                1 -> "Pull (Тяга)"
                else -> "Legs (Ноги)"
            }
            SplitType.CUSTOM -> "День ${dayIndex + 1}"
        }
    }

    private fun shouldDeload(
        settings: TrainerSettings,
        history: List<WorkoutEntry>
    ): Boolean {
        if (!settings.autoDeload) return false
        if (history.isEmpty()) return false

        val dates = history.map { parseDate(it.date) }.distinct().sorted()
        if (dates.size < 2) return false

        val firstDate = dates.first()
        val lastDate = dates.last()
        val weeksSinceStart = ChronoUnit.WEEKS.between(firstDate, lastDate)

        val interval = settings.deloadIntervalWeeks
        if (interval <= 0) return false

        return weeksSinceStart > 0 && weeksSinceStart % interval == 0L
    }

    private fun buildExerciseList(
        muscleGroups: List<MuscleGroup>,
        @Suppress("unused") priorityGroups: List<MuscleGroup>,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        progressionType: ProgressionType,
        includeWarmup: Boolean,
        isDeload: Boolean
    ): List<ExerciseRecommendation> {
        val result = mutableListOf<ExerciseRecommendation>()
        val lastSessionNames = getLastSessionExercises(muscleGroups, exercises, history)

        for (group in muscleGroups) {
            val groupExercises = exercises.filter { it.muscleGroup == group.name }
            if (groupExercises.isEmpty()) continue

            val selected = if (lastSessionNames.isNotEmpty()) {
                val fromLastSession = groupExercises.filter { it.name in lastSessionNames }
                fromLastSession.ifEmpty { groupExercises }
            } else {
                groupExercises
            }

            for (ex in selected) {
                result.add(buildExerciseRec(ex, history, trainingGoal, progressionType, includeWarmup, isDeload))
            }
        }

        return result.sortedBy {
            if (it.exercise.exerciseType == ExerciseType.COMPOUND.name) 0 else 1
        }
    }

    private fun getLastSessionExercises(
        muscleGroups: List<MuscleGroup>,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>
    ): Set<String> {
        if (history.isEmpty()) return emptySet()

        val groupNames = muscleGroups.map { it.name }.toSet()
        val relevantExerciseNames = exercises
            .filter { it.muscleGroup in groupNames }
            .map { it.name }
            .toSet()

        val relevantHistory = history.filter { it.exerciseName in relevantExerciseNames }
        if (relevantHistory.isEmpty()) return emptySet()

        val lastDate = relevantHistory.maxOf { it.date }
        return relevantHistory
            .filter { it.date == lastDate }
            .map { it.exerciseName }
            .toSet()
    }

    private fun buildExerciseRec(
        exercise: Exercise,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        progressionType: ProgressionType,
        includeWarmup: Boolean,
        isDeload: Boolean
    ): ExerciseRecommendation {
        val recentEntries = history
            .filter { it.exerciseName == exercise.name }
            .sortedByDescending { it.date }
            .take(3)

        val isCompound = exercise.exerciseType == ExerciseType.COMPOUND.name
        val weightStep = if (isCompound) 2.5 else 1.25
        val targetRange = trainingGoal.targetRange
        val workingSets = 3
        val restSeconds = getRestSeconds(trainingGoal, isCompound)

        if (recentEntries.isEmpty()) {
            val sets = (1..workingSets).map {
                SetRecommendation(
                    type = SetType.WORKING,
                    weight = null,
                    targetReps = targetRange,
                    restSeconds = restSeconds
                )
            }
            return ExerciseRecommendation(
                exercise = exercise,
                sets = sets,
                note = "Первый раз — определите рабочий вес"
            )
        }

        val lastEntry = recentEntries.first()
        val lastWeight = lastEntry.weight
        val lastReps = parseReps(lastEntry.reps)

        val (suggestedWeight, note) = calculateProgression(
            lastWeight = lastWeight,
            lastReps = lastReps,
            targetRange = targetRange,
            weightStep = weightStep,
            progressionType = progressionType,
            isDeload = isDeload
        )

        val warmupSets = if (includeWarmup && isCompound && suggestedWeight >= 20.0) {
            generateWarmupSets(suggestedWeight, targetRange)
        } else {
            emptyList()
        }

        val workingSetsList = (1..workingSets).map {
            SetRecommendation(
                type = SetType.WORKING,
                weight = suggestedWeight,
                targetReps = targetRange,
                restSeconds = restSeconds
            )
        }

        return ExerciseRecommendation(
            exercise = exercise,
            sets = warmupSets + workingSetsList,
            note = note
        )
    }

    private fun calculateProgression(
        lastWeight: Double,
        lastReps: List<Int>,
        targetRange: IntRange,
        weightStep: Double,
        progressionType: ProgressionType,
        isDeload: Boolean
    ): Pair<Double, String?> {
        if (isDeload) {
            val deloadWeight = (lastWeight * 0.6).roundToNearest(weightStep)
            return deloadWeight to "Deload-неделя: сниженный вес"
        }

        if (lastReps.isEmpty()) {
            return lastWeight to null
        }

        val avgReps = lastReps.average()
        val allInRange = lastReps.all { it in targetRange }
        val allAboveRange = lastReps.all { it >= targetRange.last }

        return when (progressionType) {
            ProgressionType.LINEAR -> {
                if (allInRange || allAboveRange) {
                    (lastWeight + weightStep) to "Прогресс: +${FormatUtils.formatWeight(weightStep)} кг"
                } else if (avgReps < targetRange.first) {
                    lastWeight to "Повторы ниже цели — оставляем вес"
                } else {
                    lastWeight to null
                }
            }
            ProgressionType.DOUBLE -> {
                if (allAboveRange) {
                    (lastWeight + weightStep) to "Все подходы на максимуме — увеличиваем вес +${FormatUtils.formatWeight(weightStep)} кг"
                } else if (allInRange) {
                    lastWeight to "Увеличивайте повторы до ${targetRange.last}"
                } else if (avgReps < targetRange.first) {
                    lastWeight to "Повторы ниже цели — работайте над техникой"
                } else {
                    lastWeight to null
                }
            }
        }
    }

    private fun generateWarmupSets(workingWeight: Double, @Suppress("unused") targetRange: IntRange): List<SetRecommendation> {
        val sets = mutableListOf<SetRecommendation>()

        val w50 = (workingWeight * 0.50).roundToNearest(2.5)
        val w75 = (workingWeight * 0.75).roundToNearest(2.5)

        if (w50 >= 10.0) {
            sets.add(SetRecommendation(SetType.WARMUP, w50, 10..12, restSeconds = 60))
        }
        if (w75 in 10.0..<workingWeight) {
            sets.add(SetRecommendation(SetType.WARMUP, w75, 5..8, restSeconds = 90))
        }

        return sets
    }

    private fun getRestSeconds(goal: TrainingGoal, isCompound: Boolean): Int {
        val base = when (goal) {
            TrainingGoal.STRENGTH -> 240
            TrainingGoal.HYPERTROPHY -> 90
            TrainingGoal.ENDURANCE -> 45
        }
        return if (isCompound) base else (base * 0.75).toInt()
    }

    private fun parseReps(repsString: String): List<Int> {
        return repsString.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DATE_FORMATTER)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    private fun Double.roundToNearest(step: Double): Double {
        return (this / step).roundToLong() * step
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
