package com.example.gymprogress.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gymprogress.MainActivity
import com.example.gymprogress.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Уведомления о сроке абонемента.
 *
 * Канал [CHANNEL_ID] изолирован от [ActiveWorkoutService.CHANNEL_ID]: здесь
 * [NotificationManager.IMPORTANCE_DEFAULT] (видимый пуш, не silent), чтобы
 * напоминание было заметным, но не будильником — по образцу Duolingo.
 */
object MembershipReminderNotifier {

    const val CHANNEL_ID = "membership_reminder"
    const val NOTIFICATION_ID = 2001

    private val RUSSIAN_DATE_FORMAT =
        DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

    /** Создаёт канал, если ещё не существует. Идемпотентно. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о абонементе",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ежедневные напоминания о окончании срока абонемента"
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Показывает уведомление о скором окончании абонемента.
     *
     * @param expiry дата окончания абонемента
     * @param daysLeft сколько дней осталось (0..6 включительно)
     */
    fun showReminder(context: Context, expiry: LocalDate, daysLeft: Int) {
        ensureChannel(context)

        val dateText = expiry.format(RUSSIAN_DATE_FORMAT)
        val contentText = if (daysLeft == 0) {
            "Абонемент заканчивается сегодня ($dateText). Не забудь продлить!"
        } else {
            "Абонемент заканчивается через $daysLeft ${dayWord(daysLeft)} ($dateText). Не забудь продлить!"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Срок абонемента на исходе")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS может быть не выдано на Android 13+.
            // Дата остаётся сохранённой; следующие сработки покажут уведомление,
            // когда пользователь выдаст разрешение.
        }
    }

    /** «день/дня/дней» — согласование с числительным. */
    private fun dayWord(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "день"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}
