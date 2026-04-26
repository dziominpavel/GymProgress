package com.example.gymprogress.data

/**
 * Диапазон дат, отображаемый на графике прогресса.
 *
 * [days] — глубина в днях относительно сегодняшней даты; `null` означает «всё время»
 * (фильтрация не применяется).
 */
enum class ChartRange(val displayName: String, val days: Int?) {
    ONE_MONTH("1М", 30),
    THREE_MONTHS("3М", 90),
    SIX_MONTHS("6М", 180),
    ONE_YEAR("1Г", 365),
    ALL("Всё", null)
}
