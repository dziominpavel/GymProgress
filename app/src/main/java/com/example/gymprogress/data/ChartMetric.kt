package com.example.gymprogress.data

/**
 * Метрика, по которой строится график прогресса.
 *
 * - [E1RM] — оценочный 1RM (кг), упрощённая оценка силы.
 * - [VOLUME] — рабочий объём подхода (вес × сумма повторов).
 * - [WORKING_WEIGHT] — рабочий вес записи (кг).
 * - [SCORE] — балл сессии 0–1000 по выбранной системе скоринга.
 */
enum class ChartMetric(val displayName: String, val unit: String) {
    E1RM("1RM", "кг"),
    VOLUME("Объём", "кг"),
    WORKING_WEIGHT("Раб. вес", "кг"),
    SCORE("Score", "")
}
