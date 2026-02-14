package com.example.gymprogress.data

enum class ProgressionType(val displayName: String, val description: String) {
    LINEAR("Плавная", "Вес растёт понемногу каждую тренировку"),
    DOUBLE("Ступенчатая", "Сначала добавляем повторы, потом увеличиваем вес")
}
