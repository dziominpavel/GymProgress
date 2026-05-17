package com.example.gymprogress.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.gymprogress.MainActivity
import com.example.gymprogress.R
import com.example.gymprogress.data.ActiveWorkoutSession
import com.example.gymprogress.data.ActiveWorkoutSnapshot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground-сервис активной тренировки.
 *
 * Стартуется из [com.example.gymprogress.ui.screens.ActiveWorkoutScreen] при входе
 * на экран и останавливается при выходе/завершении тренировки. Само состояние
 * тренировки живёт в [ActiveWorkoutSession]: Compose-стейт пишет туда снимок,
 * сервис подписывается и перерисовывает уведомление.
 *
 * Уведомление — постоянное (`ongoing`), не свайпается, показывает текущее
 * упражнение, номер подхода и таймер отдыха (если идёт). Тап по уведомлению —
 * открывает [MainActivity], чтобы вернуться к экрану тренировки.
 *
 * На Android 14+ объявлен `foregroundServiceType="health"` — это требует
 * соответствующего разрешения `FOREGROUND_SERVICE_HEALTH` в манифесте.
 */
class ActiveWorkoutService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Стартовое уведомление — без данных (state может ещё не успеть обновиться).
        val initial = ActiveWorkoutSession.state.value
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(this, initial),
            foregroundServiceType()
        )

        // Подписка на изменения снимка. Если state = null — останавливаемся.
        lifecycleScope.launch {
            ActiveWorkoutSession.state.collectLatest { snapshot ->
                if (snapshot == null) {
                    ServiceCompat.stopForeground(this@ActiveWorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                try {
                    NotificationManagerCompat.from(this@ActiveWorkoutService)
                        .notify(
                            NOTIFICATION_ID,
                            buildNotification(this@ActiveWorkoutService, snapshot)
                        )
                } catch (_: SecurityException) {
                    // Разрешение может быть отозвано во время работы сервиса.
                    // Уведомление необязательно для функциональности; продолжаем тренировку.
                }
            }
        }

        // START_NOT_STICKY: если систем убил процесс — не пересоздаём сервис автоматически.
        // Состояние тренировки и так теряется вместе с Compose-стейтом.
        return START_NOT_STICKY
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

    companion object {
        const val CHANNEL_ID = "active_workout"
        const val NOTIFICATION_ID = 1001

        /**
         * Запустить foreground-сервис из UI-кода. На API 26+ обязателен
         * `startForegroundService` — обычный `startService` бросит
         * `IllegalStateException` для foreground-сервиса в фоне.
         */
        fun start(context: Context) {
            val intent = Intent(context, ActiveWorkoutService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Остановить сервис. Делает `clear()` у [ActiveWorkoutSession], чтобы
         * подписка внутри сервиса корректно сняла уведомление и вызвала
         * [stopSelf]; запасным путём вызываем `stopService`.
         */
        fun stop(context: Context) {
            ActiveWorkoutSession.clear()
            context.stopService(Intent(context, ActiveWorkoutService::class.java))
        }

        private fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Активная тренировка",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Постоянное уведомление с прогрессом текущей тренировки"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }

        private fun buildNotification(
            context: Context,
            snapshot: ActiveWorkoutSnapshot?
        ): android.app.Notification {
            val title = snapshot?.workoutTitle?.takeIf { it.isNotBlank() } ?: "Тренировка идёт"
            val contentText = snapshot?.let { formatContent(it) } ?: "Подготовка..."

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_WORKOUT)
                .setSilent(true)
                .setContentIntent(openAppPendingIntent)

            if (snapshot != null && snapshot.overallTotalSets > 0) {
                builder.setProgress(
                    snapshot.overallTotalSets,
                    snapshot.doneSets.coerceIn(0, snapshot.overallTotalSets),
                    false
                )
            }
            return builder.build()
        }

        private fun formatContent(snapshot: ActiveWorkoutSnapshot): String {
            val parts = mutableListOf<String>()
            parts += snapshot.exerciseName
            parts += "Подход ${snapshot.setIndex}/${snapshot.totalSets}"
            snapshot.restTimeLeft?.let { rest ->
                if (rest > 0) parts += "Отдых: ${rest} сек"
            }
            if (snapshot.overallTotalSets > 0) {
                parts += "Прогресс: ${snapshot.doneSets}/${snapshot.overallTotalSets}"
            }
            return parts.joinToString(" · ")
        }
    }
}
