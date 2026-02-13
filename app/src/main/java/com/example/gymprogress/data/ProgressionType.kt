package com.example.gymprogress.data

enum class ProgressionType(val displayName: String, val description: String) {
    LINEAR("Линейная", "Увеличиваем вес каждую тренировку"),
    DOUBLE("Двойная", "Сначала повторы до верха диапазона, потом вес")
}
