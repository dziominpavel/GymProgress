package com.example.gymprogress.data

import android.util.Log
import com.example.gymprogress.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiService {

    fun isAvailable(): Boolean = BuildConfig.OPENROUTER_API_KEY.isNotBlank()

    suspend fun getAdvice(
        recommendation: WorkoutRecommendation,
        history: List<WorkoutEntry>,
        exercises: List<Exercise>,
        settings: TrainerSettings,
        trainingGoal: TrainingGoal
    ): String {
        if (!isAvailable()) return "API-ключ не настроен. Добавьте OPENROUTER_API_KEY в local.properties"

        val prompt = buildPrompt(recommendation, history, exercises, settings, trainingGoal)

        return try {
            callOpenRouter(prompt) ?: "ИИ не вернул ответ"
        } catch (e: Exception) {
            Log.w(TAG, "OpenRouter failed", e)
            "Ошибка ИИ: ${e.message}"
        }
    }

    private suspend fun callOpenRouter(prompt: String): String? = withContext(Dispatchers.IO) {
        val conn = URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("model", "meta-llama/llama-3.3-70b-instruct:free")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 1024)
                put("temperature", 0.7)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                throw Exception(errorText)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun buildPrompt(
        recommendation: WorkoutRecommendation,
        history: List<WorkoutEntry>,
        exercises: List<Exercise>,
        settings: TrainerSettings,
        trainingGoal: TrainingGoal
    ): String {
        val goalText = when (trainingGoal) {
            TrainingGoal.STRENGTH -> "сила (1-5 повторений)"
            TrainingGoal.HYPERTROPHY -> "гипертрофия (8-12 повторений)"
            TrainingGoal.ENDURANCE -> "выносливость (15-20 повторений)"
        }

        val splitText = when (settings.splitType) {
            SplitType.FULL_BODY -> "фулбади"
            SplitType.UPPER_LOWER -> "верх/низ"
            SplitType.PUSH_PULL_LEGS -> "тяни/толкай/ноги"
            SplitType.CUSTOM -> "свой сплит на ${settings.customSplitDays.size} дней"
        }

        val historyText = if (history.isEmpty()) {
            "Истории тренировок пока нет."
        } else {
            val recent = history
                .sortedByDescending { it.date }
                .take(20)
                .groupBy { it.date }
                .entries
                .joinToString("\n") { (date, entries) ->
                    val exerciseLines = entries.joinToString("; ") { e ->
                        "${e.exerciseName}: ${e.weight} кг × ${e.reps}"
                    }
                    "$date: $exerciseLines"
                }
            "Последние тренировки:\n$recent"
        }

        val recText = if (recommendation.exercises.isEmpty()) {
            "Текущая рекомендация: пусто (нет подходящих упражнений)."
        } else {
            val lines = recommendation.exercises.joinToString("\n") { rec ->
                val weight = rec.sets.firstOrNull { it.type == SetType.WORKING }?.weight
                val weightStr = if (weight != null) "${FormatUtils.formatWeight(weight)} кг" else "вес не определён"
                "- ${rec.exercise.name} ($weightStr, ${rec.sets.count { it.type == SetType.WORKING }} подходов)${rec.note?.let { " [$it]" } ?: ""}"
            }
            "Текущая рекомендация тренера на ${recommendation.dayLabel}:\n$lines"
        }

        val missingText = if (recommendation.missingGroups.isNotEmpty()) {
            "\nНе хватает упражнений для групп: ${recommendation.missingGroups.joinToString(", ") { it.displayName }}"
        } else ""

        return """
Ты — персональный фитнес-тренер в мобильном приложении. Пользователь нажал кнопку "Спросить ИИ".
Дай краткий, полезный совет на русском языке (3-5 предложений). Будь конкретным, опирайся на данные.

Цель тренировок: $goalText
Программа: $splitText
$historyText

$recText$missingText

Проанализируй данные и дай совет: прогресс, технику, восстановление, или что стоит изменить.
Не повторяй данные — пользователь их уже видит. Дай именно совет.
""".trimIndent()
    }

    companion object {
        private const val TAG = "GeminiService"
    }
}
