package com.example.gymprogress.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gymprogress.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Ежедневная проверка срока абонемента.
 *
 * Планируется [MembershipReminderScheduler] с интервалом 24 часа, когда в
 * настройках указана дата окончания. Сам Worker всегда читает актуальное
 * значение из [SettingsRepository] — если пользователь изменил/очистил дату
 * между планированием и сработкой, логика остаётся корректной.
 *
 * Окно напоминаний: `[expiry-6 .. expiry]` включительно (7 дней).
 * Если дата не указана или уже прошла — молчит.
 */
class MembershipReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = SettingsRepository(applicationContext)
        val expiry: LocalDate? = try {
            repository.membershipExpiryDate.first()
        } catch (_: Exception) {
            return Result.success()
        }

        if (expiry == null) return Result.success()

        val today = LocalDate.now(ZoneId.systemDefault())
        val daysLeft = ChronoUnit.DAYS.between(today, expiry).toInt()

        if (daysLeft < 0 || daysLeft >= 7) return Result.success()

        MembershipReminderNotifier.showReminder(applicationContext, expiry, daysLeft)
        return Result.success()
    }
}
