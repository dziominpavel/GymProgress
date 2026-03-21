package com.example.gymprogress.data

enum class ScoringSystem(val displayName: String, val description: String) {
    SIMPLIFIED(
        displayName = "Упрощённая",
        description = "Оценочный 1RM (кг) по формуле Epley с лестницей усилия. Прогресс = динамика 1RM."
    ),
    ADVANCED(
        displayName = "Усложнённая",
        description = "Составной стимул (напряжение + продуктивность + качество). Шкала 0–1000."
    )
}
