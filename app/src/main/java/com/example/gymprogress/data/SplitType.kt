package com.example.gymprogress.data

enum class SplitType(val displayName: String, val description: String) {
    FULL_BODY("Full Body", "Все группы мышц за одну тренировку"),
    UPPER_LOWER("Upper / Lower", "Чередование верха и низа тела"),
    PUSH_PULL_LEGS("Push / Pull / Legs", "Жимовые / Тяговые / Ноги"),
    CUSTOM("Свой сплит", "Настройте дни и группы вручную")
}
