package com.example.gymprogress.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Планировщик ежедневной проверки срока абонемента.
 *
 * Использует [WorkManager.enqueueUniquePeriodicWork] с интервалом 24 часа.
 * Первая сработка — в ближайшее 12:00 локального времени (через [initialDelay]).
 * WorkManager переживает reboot без дополнительных разрешений.
 *
 * Вызывается из [com.example.gymprogress.viewmodel.WorkoutViewModel] при
 * изменении `membershipExpiryDate`:
 * - не `null` → [schedule]
 * - `null` → [cancel]
 */
object MembershipReminderScheduler {

    const val WORK_NAME = "membership_reminder_daily"

    fun schedule(context: Context) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val nextNoon = LocalDate.now(ZoneId.systemDefault())
            .atTime(LocalTime.NOON)
            .atZone(ZoneId.systemDefault())
            .let { noon ->
                if (noon.isAfter(now)) noon else noon.plusDays(1)
            }
        val initialDelayMinutes = ChronoUnit.MINUTES.between(now, nextNoon).coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<MembershipReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
