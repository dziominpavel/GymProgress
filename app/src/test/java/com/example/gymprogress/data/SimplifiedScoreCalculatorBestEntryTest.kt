package com.example.gymprogress.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SimplifiedScoreCalculatorBestEntryTest {

    private fun e1rm(weight: Double, reps: String): Double =
        SimplifiedScoreCalculator.calcE1RMForEntry(
            WorkoutEntry(id = 0L, date = "2026-01-01", exerciseName = "X", weight = weight, reps = reps),
            bodyWeightKg = null,
            isBodyweightExercise = false
        )

    private fun assertClose(expected: Double, actual: Double, eps: Double = 0.01) {
        assertTrue("expected=$expected actual=$actual", abs(expected - actual) <= eps)
    }

    // ── Улучшение 1: гибрид Epley/Brzycki ─────────────────────────────────

    @Test
    fun repMaxPerSet_uses_epley_for_reps_up_to_10() {
        // Epley: 100 × (1 + 5/30) = 116.667
        assertClose(116.667, e1rm(100.0, "5"))
        // Граница reps=10: 100 × (1 + 10/30) = 133.333
        assertClose(133.333, e1rm(100.0, "10"))
    }

    @Test
    fun repMaxPerSet_uses_brzycki_above_10_reps() {
        // Brzycki: 100 × 36 / (37 - 12) = 144.0
        assertClose(144.0, e1rm(100.0, "12"))
        // Brzycki при reps=15: 100 × 36 / 22 = 163.636
        assertClose(163.636, e1rm(100.0, "15"))
    }

    @Test
    fun repMaxPerSet_caps_brzycki_reps_at_15() {
        // При reps=20 Brzycki капается на 15: 100 × 36 / 22 = 163.636
        assertClose(163.636, e1rm(100.0, "20"))
    }

    @Test
    fun repMaxPerSet_crossover_at_reps_10_is_continuous() {
        // Epley(10) == Brzycki(10) == 1.3333 × weight
        val epleyAt10 = 100.0 * (1.0 + 10.0 / 30.0)
        val brzyckiAt10 = 100.0 * 36.0 / (37.0 - 10.0)
        assertClose(epleyAt10, brzyckiAt10)
    }

    // ── Улучшение 2: бонус за подтверждающие подходы ──────────────────────

    @Test
    fun volumeBonus_boosts_repeated_sets_over_single_set() {
        // 1×5 @ 50 → 58.333 (без бонуса)
        val single = e1rm(50.0, "5")
        // 3×5 @ 50 → raw одинаковые, confirming=3, bonus=0.03. best=50×1.1667×1.00=58.333.
        // final = 58.333 × 1.03 = 60.083
        val triple = e1rm(50.0, "5,5,5")
        assertClose(58.333, single)
        assertClose(60.083, triple)
        assertTrue("3×5 должно быть больше 1×5", triple > single)
    }

    @Test
    fun volumeBonus_caps_at_5_percent_regardless_of_set_count() {
        // 5×5 @ 50: confirming=5 → bonus = min(5%, 1.5%×4) = 5%
        // 10×5 @ 50: confirming=10 → bonus = min(5%, 1.5%×9) = 5% (cap)
        val fiveSets = e1rm(50.0, "5,5,5,5,5")
        val tenSets = e1rm(50.0, "5,5,5,5,5,5,5,5,5,5")
        // best (adjusted, последний) = 58.333; с бонусом 5%: 61.25
        assertClose(61.25, fiveSets)
        assertClose(61.25, tenSets)
    }

    @Test
    fun volumeBonus_ignores_non_confirming_sets_below_95_percent() {
        // 50×(12,10,8): raw=[72, 66.67, 63.33]. bestRaw=72. Порог 68.4: только 72 подтверждает.
        // confirming=1 → bonus=0. adjusted=[67.68, 64.67, 63.33], best=67.68.
        // Но лучший — первый подход → применяется штраф за усталость (см. тест ниже).
        // Здесь проверим только что нет бонуса: без штрафа было бы 67.68.
        val session = e1rm(50.0, "12,10,8")
        // dropRate = 1 - 8/12 = 0.333 → penalty 0.03 × 0.5 = 0.015
        // final = 67.68 × (1 - 0.015) = 66.665
        assertClose(66.665, session)
    }

    // ── Улучшение 3: умный штраф за усталость ─────────────────────────────

    @Test
    fun smartFatiguePenalty_applied_when_best_set_is_not_last() {
        // 50×(12,10,8): лучший подход — первый (adjusted=67.68), последний хуже.
        // calcFatiguePenalty: dropRate=0.333 → 0.03; smart = 0.03×0.5 = 0.015.
        // final = 67.68 × (1 - 0.015) = 66.665 (без штрафа было бы 67.68).
        val withPenalty = e1rm(50.0, "12,10,8")
        assertClose(66.665, withPenalty)
        assertTrue("Штраф должен снизить E1RM ниже best=67.68", withPenalty < 67.68)
    }

    @Test
    fun smartFatiguePenalty_not_applied_when_last_set_is_best() {
        // 50×(8,10,12): последний подход самый сильный → штраф не применяется.
        // raw=[63.33, 66.67, 72]. adjusted=[59.53, 64.67, 72]. best=72 (last).
        // confirming: только 72 ≥ 72×0.95=68.4 → 1 set, bonus=0.
        val noPenalty = e1rm(50.0, "8,10,12")
        assertClose(72.0, noPenalty)
    }

    @Test
    fun smartFatiguePenalty_not_applied_for_equal_sets() {
        // 50×(10,10,10): dropRate=0, best=last. Никаких штрафов.
        // raw=[66.67×3]. confirming=3. bonus=0.03. final=66.67×1.03=68.667.
        val equalSets = e1rm(50.0, "10,10,10")
        assertClose(68.667, equalSets)
    }

    // ── Существующие тесты поведения сохранены ────────────────────────────


    @Test
    fun bestEntryByEstimatedE1RM_picks_session_with_higher_e1rm() {
        val weak = WorkoutEntry(
            id = 1L,
            date = "2026-01-01",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "10,10,10"
        )
        val strong = WorkoutEntry(
            id = 2L,
            date = "2026-01-02",
            exerciseName = "Жим",
            weight = 36.0,
            reps = "5"
        )
        val list = listOf(weak, strong)
        val best = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            list,
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(strong.id, best?.id)
    }

    @Test
    fun bestEntryByEstimatedE1RM_tie_prefers_newer_date_then_higher_id() {
        val older = WorkoutEntry(
            id = 1L,
            date = "2026-01-01",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val newer = WorkoutEntry(
            id = 2L,
            date = "2026-01-10",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val sameDayLaterId = WorkoutEntry(
            id = 3L,
            date = "2026-01-10",
            exerciseName = "Жим",
            weight = 20.0,
            reps = "5"
        )
        val bestDate = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            listOf(older, newer),
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(newer.id, bestDate?.id)

        val bestId = SimplifiedScoreCalculator.bestEntryByEstimatedE1RM(
            listOf(newer, sameDayLaterId),
            bodyWeightKg = null,
            isBodyweightExercise = false
        )
        assertEquals(sameDayLaterId.id, bestId?.id)
    }
}
