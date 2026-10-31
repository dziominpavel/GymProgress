package com.example.gymprogress.data

/**
 * Выбирает каноническое упражнение для каждого нормализованного имени.
 *
 * Приоритет: больше связанных записей, затем меньший `id`. Используется
 * миграцией при слиянии исторических дубликатов и покрыт unit-тестом.
 */
internal fun canonicalExercisesByKey(
    exercises: List<Exercise>,
    entryCountByExerciseId: Map<Long, Int>
): Map<String, Exercise> {
    return exercises
        .groupBy { it.nameKey }
        .mapValues { (_, group) ->
            group.maxWith(
                compareBy<Exercise> { entryCountByExerciseId[it.id] ?: 0 }
                    .thenByDescending { it.id }
            )
        }
}
