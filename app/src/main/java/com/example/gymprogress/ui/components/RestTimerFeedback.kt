package com.example.gymprogress.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Обратная связь для таймера отдыха активной тренировки.
 *
 * - [shortBeep] — короткий бип за 3/2/1 сек до конца отдыха.
 * - [longBeep] — длинный бип в момент окончания отдыха.
 * - [vibrate] — короткая вибрация в момент окончания отдыха (≈300 мс).
 *
 * Звук — через [ToneGenerator] на стандартных тонах (без необходимости класть
 * аудио-ресурсы в проект и без зависимости от текущей громкости звонка). Если
 * системный звуковой стек недоступен (редкий случай при тестах/эмуляторах),
 * экземпляр генератора будет `null` и звук просто не сыграет.
 *
 * Вибрация — через [VibratorManager] на API 31+ или [Vibrator] для более ранних
 * API. Если вибромотор отсутствует, метод тихо ничего не делает.
 */
class RestTimerFeedback internal constructor(
    private val context: Context,
    private val toneGenerator: ToneGenerator?
) {
    fun shortBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, SHORT_BEEP_MS)
    }

    fun longBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, LONG_BEEP_MS)
    }

    fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_MS)
        }
    }

    internal fun release() {
        toneGenerator?.release()
    }

    private companion object {
        const val SHORT_BEEP_MS = 120
        const val LONG_BEEP_MS = 450
        const val VIBRATION_MS = 300L
    }
}

/**
 * Создаёт [RestTimerFeedback] на жизненный цикл вызывающего composable.
 * При уходе с экрана генератор тонов корректно освобождается.
 */
@Composable
fun rememberRestTimerFeedback(): RestTimerFeedback {
    val context = LocalContext.current
    val feedback = remember(context) {
        val tone = try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)
        } catch (_: RuntimeException) {
            null
        }
        RestTimerFeedback(context, tone)
    }
    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }
    return feedback
}

private const val TONE_VOLUME = 80
