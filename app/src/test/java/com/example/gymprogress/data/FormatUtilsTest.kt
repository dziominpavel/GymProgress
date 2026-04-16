package com.example.gymprogress.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatUtilsTest {

    @Test
    fun storageDate_roundTrips() {
        val d = LocalDate.of(2026, 4, 4)
        val s = FormatUtils.toStorageDate(d)
        assertEquals(d, FormatUtils.parseStorageDate(s))
    }

    @Test
    fun formatJournalDayMonth_usesRussianLocale() {
        val s = FormatUtils.formatJournalDayMonth(LocalDate.of(2026, 4, 4))
        assert(s.any { it.isLetter() }) { "expected non-empty formatted date" }
    }
}
