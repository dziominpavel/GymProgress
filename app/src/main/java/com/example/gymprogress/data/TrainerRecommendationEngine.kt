package com.example.gymprogress.data

import kotlin.math.roundToLong
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TrainerRecommendationEngine {

    fun generateRecommendation(
        settings: TrainerSettings,
        trainingGoal: TrainingGoal,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>,
        bodyWeightKg: Double? = null,
        scoringEngine: ScoringEngine = SimplifiedScoreCalculator,
        scoringSystem: ScoringSystem = ScoringSystem.SIMPLIFIED
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
                isDeload = isDeload,
                bodyWeightKg = bodyWeightKg,
                scoringEngine = scoringEngine,
                scoringSystem = scoringSystem
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

    /**
     * Рекомендация по одному упражнению для отображения в журнале (под «Лучшая тренировка»).
     * Не привязана к дню сплита — используется последняя по дате запись как ориентир.
     */
    fun getRecommendationForExercise(
        exercise: Exercise,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        settings: TrainerSettings,
        bodyWeightKg: Double?,
        scoringEngine: ScoringEngine,
        scoringSystem: ScoringSystem
    ): ExerciseRecommendation {
        val isDeload = shouldDeload(settings, history)
        return buildExerciseRec(
            exercise = exercise,
            history = history,
            trainingGoal = trainingGoal,
            progressionType = settings.progressionType,
            includeWarmup = settings.includeWarmup,
            isDeload = isDeload,
            lastSessionDate = null,
            bodyWeightKg = bodyWeightKg,
            scoringEngine = scoringEngine,
            scoringSystem = scoringSystem
        )
    }

    /**
     * Находит последнюю по дате сессию, соответствующую указанному дню сплита.
     * Используется для авто-режима (предыдущий день) и ручного выбора дня в журнале.
     */
    fun findSessionForDayIndex(
        settings: TrainerSettings,
        history: List<WorkoutEntry>,
        exercises: List<Exercise>,
        dayIndex: Int
    ): PreviousSessionInSplit? {
        if (history.isEmpty()) return null
        val totalDays = getTotalDays(settings)
        if (dayIndex !in 0 until totalDays) return null
        // Для ручного выбора дня в журнале тоже ориентируемся только на завершённые
        // сессии, игнорируя текущий незавершённый день (как и в авто-режиме).
        val rotationHistory = historyForRotation(history)
        if (rotationHistory.isEmpty()) return null

        val byDate = rotationHistory.groupBy { it.date }
        val sortedDates = byDate.keys
            .mapNotNull { d -> FormatUtils.parseStorageDate(d)?.let { d to it } }
            .sortedByDescending { it.second }
            .map { it.first }

        for (date in sortedDates) {
            val sessionEntries = byDate[date] ?: continue
            val exerciseNames = sessionEntries.map { it.exerciseName }.toSet()
            val sessionDayIndex = guessDayIndexFromExercises(settings, exerciseNames, exercises)
            if (sessionDayIndex == dayIndex) {
                val dayLabel = getDayLabel(settings, dayIndex)
                return PreviousSessionInSplit(
                    entries = sessionEntries.sortedBy { it.id },
                    date = date,
                    dayLabel = dayLabel
                )
            }
        }
        return null
    }

    /**
     * Находит последнюю сессию **следующего** дня сплита (того, что будет на следующей тренировке).
     * В журнале по умолчанию показываем именно её — чтобы видеть, что делали в прошлый раз в этот день.
     */
    fun findNextDaySessionInSplit(
        settings: TrainerSettings,
        history: List<WorkoutEntry>,
        exercises: List<Exercise>
    ): PreviousSessionInSplit? {
        if (history.isEmpty()) return null
        val totalDays = getTotalDays(settings)

        // В авто-режиме журнала ориентируемся на последнюю завершённую тренировку,
        // игнорируя незавершённый сегодняшний день, чтобы сплит не «скакал»
        // во время текущей сессии.
        val rotationHistory = historyForRotation(history)
        if (rotationHistory.isEmpty()) return null

        val lastDate = rotationHistory.maxOfOrNull { parseDate(it.date) } ?: return null
        val lastStorageDate = FormatUtils.toStorageDate(lastDate)
        val lastExerciseNames = rotationHistory
            .filter { it.date == lastStorageDate }
            .map { it.exerciseName }
            .toSet()
        val lastDayIndex = guessDayIndexFromExercises(settings, lastExerciseNames, exercises)
        val nextDayIndex = (lastDayIndex + 1) % totalDays

        return findSessionForDayIndex(settings, rotationHistory, exercises, nextDayIndex)
    }

    /** Возвращает группы мышц для дня сплита (для отображения в UI). */
    fun getMuscleGroupsForDayPublic(settings: TrainerSettings, dayIndex: Int): List<MuscleGroup> {
        return getMuscleGroupsForDay(settings, dayIndex)
    }

    /** Индекс следующего дня сплита (какой будет на следующей тренировке). */
    fun getNextDayIndex(
        settings: TrainerSettings,
        history: List<WorkoutEntry>,
        exercises: List<Exercise>
    ): Int {
        if (history.isEmpty()) return 0
        val totalDays = getTotalDays(settings)
        if (totalDays == 1) return 0

        // Здесь тоже опираемся на последнюю завершённую тренировку.
        val rotationHistory = historyForRotation(history)
        if (rotationHistory.isEmpty()) return 0

        val lastDate = rotationHistory.maxOfOrNull { parseDate(it.date) } ?: return 0
        val lastStorageDate = FormatUtils.toStorageDate(lastDate)
        val lastExerciseNames = rotationHistory
            .filter { it.date == lastStorageDate }
            .map { it.exerciseName }
            .toSet()
        val lastDayIndex = guessDayIndexFromExercises(settings, lastExerciseNames, exercises)
        return (lastDayIndex + 1) % totalDays
    }

    fun getSplitDayOptions(settings: TrainerSettings): List<Pair<Int, String>> {
        val totalDays = getTotalDays(settings)
        if (totalDays <= 1) return emptyList()
        return (0 until totalDays).map { i -> i to getDayLabel(settings, i) }
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

        // Для рекомендации тренера также считаем следующий день по последней
        // завершённой тренировке, чтобы во время текущего дня сплит не смещался.
        val rotationHistory = historyForRotation(history)
        if (rotationHistory.isEmpty()) return 0

        val lastDate = rotationHistory.maxOfOrNull { parseDate(it.date) } ?: return 0
        val lastExerciseNames = rotationHistory
            .filter { it.date == FormatUtils.toStorageDate(lastDate) }
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
        isDeload: Boolean,
        bodyWeightKg: Double?,
        scoringEngine: ScoringEngine,
        scoringSystem: ScoringSystem
    ): List<ExerciseRecommendation> {
        val result = mutableListOf<ExerciseRecommendation>()
        val lastSessionNames = getLastSessionExercises(muscleGroups, exercises, history)
        val lastSessionOrder = getLastSessionExerciseOrder(muscleGroups, exercises, history)
        val lastSessionDate = getLastSessionDate(muscleGroups, exercises, history)

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
                result.add(
                    buildExerciseRec(
                        exercise = ex,
                        history = history,
                        trainingGoal = trainingGoal,
                        progressionType = progressionType,
                        includeWarmup = includeWarmup,
                        isDeload = isDeload,
                        lastSessionDate = lastSessionDate,
                        bodyWeightKg = bodyWeightKg,
                        scoringEngine = scoringEngine,
                        scoringSystem = scoringSystem
                    )
                )
            }
        }

        // Сначала в порядке прошлой сессии (как вводил пользователь), остальные — compound первыми
        if (lastSessionOrder.isNotEmpty()) {
            val orderMap = lastSessionOrder.mapIndexed { i, name -> name to i }.toMap()
            val inOrder = result.filter { it.exercise.name in orderMap }
            val inOrderSorted = lastSessionOrder.mapNotNull { name ->
                inOrder.find { it.exercise.name == name }
            }
            val rest = result.filter { it.exercise.name !in orderMap }
                .sortedBy { if (it.exercise.exerciseType == ExerciseType.COMPOUND.name) 0 else 1 }
            return inOrderSorted + rest
        }
        return result.sortedBy {
            if (it.exercise.exerciseType == ExerciseType.COMPOUND.name) 0 else 1
        }
    }

    /** Порядок упражнений в прошлой сессии (по id записей = порядок ввода). */
    private fun getLastSessionExerciseOrder(
        muscleGroups: List<MuscleGroup>,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>
    ): List<String> {
        if (history.isEmpty()) return emptyList()

        val groupNames = muscleGroups.map { it.name }.toSet()
        val relevantExerciseNames = exercises
            .filter { it.muscleGroup in groupNames }
            .map { it.name }
            .toSet()

        val relevantHistory = history.filter { it.exerciseName in relevantExerciseNames }
        if (relevantHistory.isEmpty()) return emptyList()

        val lastDate = relevantHistory.maxOf { it.date }
        return relevantHistory
            .filter { it.date == lastDate }
            .sortedBy { it.id }
            .map { it.exerciseName }
            .distinct()
    }

    private fun getLastSessionExercises(
        muscleGroups: List<MuscleGroup>,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>
    ): Set<String> {
        return getLastSessionExerciseOrder(muscleGroups, exercises, history).toSet()
    }

    private fun getLastSessionDate(
        muscleGroups: List<MuscleGroup>,
        exercises: List<Exercise>,
        history: List<WorkoutEntry>
    ): String? {
        if (history.isEmpty()) return null

        val groupNames = muscleGroups.map { it.name }.toSet()
        val relevantExerciseNames = exercises
            .filter { it.muscleGroup in groupNames }
            .map { it.name }
            .toSet()

        val relevantHistory = history.filter { it.exerciseName in relevantExerciseNames }
        val lastEntry = relevantHistory.maxByOrNull { parseDate(it.date) } ?: return null
        return lastEntry.date
    }

    private fun buildExerciseRec(
        exercise: Exercise,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        progressionType: ProgressionType,
        includeWarmup: Boolean,
        isDeload: Boolean,
        lastSessionDate: String?,
        bodyWeightKg: Double?,
        scoringEngine: ScoringEngine,
        scoringSystem: ScoringSystem
    ): ExerciseRecommendation {
        val exerciseHistory = history
            .filter { it.exerciseName == exercise.name }
            .sortedByDescending { it.date }

        val isCompound = exercise.exerciseType == ExerciseType.COMPOUND.name
        val weightStep = if (isCompound) 2.5 else 1.25
        val targetRange = trainingGoal.targetRange
        val restSeconds = getRestSeconds(trainingGoal, isCompound)

        if (exerciseHistory.isEmpty()) {
            val baseWorkingSets = 3
            val sets = (1..baseWorkingSets).map {
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
                note = "Первый раз — определите рабочий вес",
                advice = "Начните в диапазоне ${targetRange.first}–${targetRange.last} повторов"
            )
        }

        val lastEntry = exerciseHistory.firstOrNull { it.date == lastSessionDate }
            ?: exerciseHistory.first()
        val lastWeight = lastEntry.weight
        val lastReps = parseReps(lastEntry.reps)

        val exerciseType = ExerciseType.entries
            .find { it.name == exercise.exerciseType } ?: ExerciseType.COMPOUND
        val isBw = exercise.isBodyweight
        val bestEntry = selectBestSessionEntry(
            exerciseHistory,
            scoringEngine,
            scoringSystem,
            trainingGoal,
            exerciseType,
            bodyWeightKg,
            isBw
        )

        val (suggestedWeight, note) = calculateProgression(
            lastWeight = lastWeight,
            lastReps = lastReps,
            targetRange = targetRange,
            weightStep = weightStep,
            progressionType = progressionType,
            isDeload = isDeload
        )

        val progressSnapshot = buildProgressSnapshot(
            lastEntry = lastEntry,
            previousEntry = exerciseHistory.getOrNull(1),
            history = exerciseHistory,
            trainingGoal = trainingGoal,
            exerciseType = exerciseType,
            targetRange = targetRange,
            bodyWeightKg = bodyWeightKg,
            scoringEngine = scoringEngine,
            isBodyweightExercise = isBw
        )

        val (workingSets, volumeNote) = determineWorkingSets(
            isDeload = isDeload,
            snapshot = progressSnapshot
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

        val advice = buildAdvice(
            lastEntry = lastEntry,
            bestEntry = bestEntry,
            history = exerciseHistory,
            trainingGoal = trainingGoal,
            exerciseType = exerciseType,
            weightStep = weightStep,
            snapshot = progressSnapshot,
            volumeNote = volumeNote,
            bodyWeightKg = bodyWeightKg,
            scoringEngine = scoringEngine,
            isBodyweightExercise = isBw
        )

        return ExerciseRecommendation(
            exercise = exercise,
            sets = warmupSets + workingSetsList,
            note = note,
            lastEntry = lastEntry,
            bestEntry = bestEntry,
            advice = advice
        )
    }

    private fun buildAdvice(
        lastEntry: WorkoutEntry,
        bestEntry: WorkoutEntry?,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        exerciseType: ExerciseType,
        weightStep: Double,
        snapshot: ProgressSnapshot,
        volumeNote: String?,
        bodyWeightKg: Double?,
        scoringEngine: ScoringEngine,
        isBodyweightExercise: Boolean
    ): String? {
        if (bestEntry == null) return null

        val lastScore = scoringEngine
            .calcSessionScore(
                lastEntry, history, trainingGoal, exerciseType, bodyWeightKg, isBodyweightExercise
            )
            .score
        val bestScore = scoringEngine
            .calcSessionScore(
                bestEntry, history, trainingGoal, exerciseType, bodyWeightKg, isBodyweightExercise
            )
            .score

        val lastReps = parseReps(lastEntry.reps)
        val targetRange = trainingGoal.targetRange

        val baseAdvice = if (bestEntry.id == lastEntry.id) {
            if (lastReps.isNotEmpty() && lastReps.all { it in targetRange }) {
                "Попробуйте +${FormatUtils.formatWeight(weightStep)} кг при тех же повторах"
            } else {
                "Цель: +1 повтор в последнем подходе"
            }
        } else if (bestScore - lastScore >= 15) {
            val bestReps = parseReps(bestEntry.reps)
            val repsLabel = if (bestReps.isNotEmpty()) bestReps.joinToString(" · ") else ""
            if (repsLabel.isNotEmpty()) {
                "Ориентир: ${FormatUtils.formatWeight(bestEntry.weight)} кг и $repsLabel повт."
            } else {
                "Ориентир: ${FormatUtils.formatWeight(bestEntry.weight)} кг"
            }
        } else if (snapshot.isStagnating && snapshot.isStableInRange) {
            "Если 2 тренировки без прогресса — откатите вес на 5% и вернитесь"
        } else {
            "Сделайте чуть лучше прошлой: +1 повтор или +${FormatUtils.formatWeight(weightStep)} кг"
        }

        return listOfNotNull(baseAdvice, volumeNote).joinToString(" • ")
    }

    private data class ProgressSnapshot(
        val lastScore: SessionScore,
        val previousScore: SessionScore?,
        val isStableInRange: Boolean,
        val isLowFatigue: Boolean,
        val isHighFatigue: Boolean,
        val isStagnating: Boolean,
        val isImproving: Boolean
    )

    private fun buildProgressSnapshot(
        lastEntry: WorkoutEntry,
        previousEntry: WorkoutEntry?,
        history: List<WorkoutEntry>,
        trainingGoal: TrainingGoal,
        exerciseType: ExerciseType,
        targetRange: IntRange,
        bodyWeightKg: Double?,
        scoringEngine: ScoringEngine,
        isBodyweightExercise: Boolean
    ): ProgressSnapshot {
        val lastScore = scoringEngine.calcSessionScore(
            lastEntry,
            history,
            trainingGoal,
            exerciseType,
            bodyWeightKg,
            isBodyweightExercise
        )
        val previousScore = previousEntry?.let {
            scoringEngine.calcSessionScore(
                it,
                history,
                trainingGoal,
                exerciseType,
                bodyWeightKg,
                isBodyweightExercise
            )
        }

        val lastReps = parseReps(lastEntry.reps)
        val isStableInRange = lastReps.isNotEmpty() && lastReps.all { it in targetRange }
        val isLowFatigue = lastScore.fatiguePenalty <= 0.07
        val isHighFatigue = lastScore.fatiguePenalty >= 0.08 || lastScore.repQuality < 0.6
        val isImproving = previousScore != null && lastScore.score - previousScore.score >= 5
        val isStagnating = previousScore != null && kotlin.math.abs(lastScore.score - previousScore.score) < 3

        return ProgressSnapshot(
            lastScore = lastScore,
            previousScore = previousScore,
            isStableInRange = isStableInRange,
            isLowFatigue = isLowFatigue,
            isHighFatigue = isHighFatigue,
            isStagnating = isStagnating,
            isImproving = isImproving
        )
    }

    private fun determineWorkingSets(
        isDeload: Boolean,
        snapshot: ProgressSnapshot
    ): Pair<Int, String?> {
        if (isDeload) return 2 to "Deload: 2 рабочих подхода"

        return when {
            snapshot.isHighFatigue && !snapshot.isImproving -> 2 to "Снижаем объём до 2 подходов для восстановления"
            snapshot.isImproving && snapshot.isStableInRange && snapshot.isLowFatigue ->
                4 to "Можно добавить 4-й рабочий подход"
            else -> 3 to null
        }
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
        return FormatUtils.parseStorageDate(dateString) ?: LocalDate.now()
    }

    /**
     * История, по которой считаем «последний завершённый день» для сплита.
     * Если есть тренировки до сегодняшнего дня — используем только их
     * (игнорируя текущий незавершённый день). Если в истории только сегодня —
     * возвращаем все записи как есть.
     */
    private fun historyForRotation(history: List<WorkoutEntry>): List<WorkoutEntry> {
        if (history.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val past = history.filter { parseDate(it.date).isBefore(today) }
        return if (past.isNotEmpty()) past else history
    }

    private fun Double.roundToNearest(step: Double): Double {
        return (this / step).roundToLong() * step
    }
}
