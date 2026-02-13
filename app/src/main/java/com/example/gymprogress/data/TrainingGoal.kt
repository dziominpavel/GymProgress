package com.example.gymprogress.data

enum class TrainingGoal(
    val displayName: String,
    val description: String,
    val targetRange: IntRange,
    val nearRange: IntRange,
    val farRange: IntRange
) {
    HYPERTROPHY(
        displayName = "Гипертрофия",
        description = "Набор мышечной массы (8–12 повторов)",
        targetRange = 8..12,
        nearRange = 6..15,
        farRange = 1..30
    ),
    STRENGTH(
        displayName = "Сила",
        description = "Увеличение силовых показателей (3–6 повторов)",
        targetRange = 3..6,
        nearRange = 1..8,
        farRange = 1..30
    ),
    ENDURANCE(
        displayName = "Выносливость",
        description = "Мышечная выносливость (15–25 повторов)",
        targetRange = 15..25,
        nearRange = 12..30,
        farRange = 1..40
    )
}
