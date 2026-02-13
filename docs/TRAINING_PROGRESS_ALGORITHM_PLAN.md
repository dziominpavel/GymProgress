# Алгоритм оценки прогресса тренировок

## 1. Контекст и задача

Приложение хранит записи: **упражнение, вес, подходы (массив повторений), дата**.
Задача: по двум записям одного и того же упражнения определить, какая тренировка **лучше** с точки зрения выбранной цели.

Алгоритм должен работать для любого пользователя, но веса компонентов настраиваются под цель.

---

## 2. Входные данные

### 2.1. Уже есть в БД (без миграций)
| Поле | Пример |
|------|--------|
| `exerciseName` | "Жим лёжа" |
| `weight` | 60.0 (кг) |
| `reps` | "10,10,10,10" |
| `date` | "13.02.2026" |

### 2.2. Производные (вычисляются на лету)
```
sets       = reps.size                          // 4
totalReps  = reps.sum()                         // 40
volume     = weight × totalReps                 // 2400
avgReps    = totalReps / sets                   // 10.0
```

### 2.3. Дополнительные поля (Этап 2+)
| Поле | Зачем |
|------|-------|
| `bodyWeight` | Для BW-упражнений: `effectiveLoad = bodyWeight + addedWeight` |
| `exerciseType` | `compound` / `isolation` — разные коэффициенты формулы |
| `goal` | `hypertrophy` / `strength` / `endurance` — разные целевые диапазоны |

> **RPE/RIR** (насколько близко к отказу) — полезно, но на MVP не обязательно. Добавляется на Этапе 3.

---

## 3. Алгоритм скоринга

Скоринг каждой записи состоит из **трёх компонентов** и одного **штрафа**.

### 3.1. Компонент A — Объём (Volume Score)

Общий объём нагрузки — ключевой драйвер гипертрофии.

```
volume = weight × totalReps

volumeScore = volume / maxVolume
```

Где `maxVolume` — максимальный объём среди последних **N** записей этого упражнения (окно: N = 12, или все записи, если их < 12).

Результат: значение в диапазоне `(0.0 .. 1.0]`.

### 3.2. Компонент B — Интенсивность (Intensity Score)

Рабочий вес — показатель силовой нагрузки на мышцу.

```
intensityScore = weight / maxWeight
```

Где `maxWeight` — максимальный вес среди тех же последних N записей.

Для BW-упражнений: `weight` заменяется на `effectiveLoad = bodyWeight + addedWeight`.

Результат: `(0.0 .. 1.0]`.

### 3.3. Компонент C — Качество повторений (Rep Quality)

Определяет, попадают ли повторения в **целевой диапазон** для выбранной цели.

#### Целевые диапазоны по цели:
| Цель | Идеально | Допустимо | Плохо |
|------|----------|-----------|-------|
| Гипертрофия | 8–12 | 6–7, 13–15 | ≤5, ≥16 |
| Сила | 3–6 | 1–2, 7–8 | ≥9 |
| Выносливость | 15–25 | 12–14, 26–30 | ≤11, ≥31 |

#### Формула для каждого подхода:
```
setQuality(reps):
    если reps в идеальном диапазоне   → 1.0
    если reps в допустимом диапазоне  → 0.6
    иначе                             → 0.3
```

#### Итоговое качество:
```
repQuality = среднее(setQuality по всем подходам)
```

Результат: `[0.3 .. 1.0]`.

### 3.4. Штраф за просадку (Fatigue Penalty)

Если повторения сильно падают от первого к последнему подходу, это снижает качество тренировки (чрезмерная усталость или завышенный вес).

```
dropRate = 1.0 - (lastSetReps / firstSetReps)

fatiguePenalty:
    если dropRate ≤ 0.20  → 0.00   // нормальное падение
    если dropRate ≤ 0.35  → 0.05   // умеренное
    если dropRate ≤ 0.50  → 0.10   // значительное
    иначе                 → 0.15   // чрезмерное
```

> Если всего 1 подход — `fatiguePenalty = 0`.

### 3.5. Итоговый балл сессии

```
sessionScore = Wv × volumeScore + Wi × intensityScore + Wr × repQuality − fatiguePenalty
```

#### Веса по цели и типу упражнения:

| Цель | Тип | Wv (объём) | Wi (интенсивность) | Wr (качество) |
|------|-----|------------|---------------------|----------------|
| Гипертрофия | compound | 0.45 | 0.25 | 0.30 |
| Гипертрофия | isolation | 0.35 | 0.15 | 0.50 |
| Сила | compound | 0.25 | 0.45 | 0.30 |
| Сила | isolation | 0.20 | 0.40 | 0.40 |
| Выносливость | any | 0.50 | 0.10 | 0.40 |

> **MVP (Этап 1):** без `exerciseType` и `goal` используем строку **Гипертрофия / compound** как дефолт.

Итог ограничиваем: `sessionScore = clamp(sessionScore, 0.0, 1.0)`.

---

## 4. Сравнение двух тренировок

Сравниваем **текущую** запись с **предыдущей** записью того же упражнения.

```
delta = currentScore − previousScore

если delta ≥ +0.03  → ▲ ЛУЧШЕ
если delta ≤ −0.03  → ▼ ХУЖЕ
иначе               → → БЕЗ ИЗМЕНЕНИЙ
```

### Процент изменения
```
deltaPercent = ((currentScore − previousScore) / previousScore) × 100
```

Отображение: `+6.4%` или `-3.1%`.

### Автоматическое определение причины
```
volumeDelta    = currentVolume − prevVolume
intensityDelta = currentWeight − prevWeight
qualityDelta   = currentRepQuality − prevRepQuality

Наибольший по модулю дельта-компонент определяет причину:
- volumeDelta     → "Объём +12%"
- intensityDelta  → "+5 кг"
- qualityDelta    → "Качество подходов выше/ниже"

Дополнение: если repQuality < 0.6 → добавить "повторы вне диапазона 8–12"
```

---

## 5. Примеры расчёта

### Пример 1: Жим лёжа (гипертрофия, compound)

**Запись A:** 60 кг × [10, 10, 10, 10]
```
volume     = 60 × 40 = 2400
totalReps  = 40,  avgReps = 10
repQuality = avg(1.0, 1.0, 1.0, 1.0) = 1.0
dropRate   = 1 - 10/10 = 0  → penalty = 0
```

**Запись B:** 65 кг × [8, 7, 6, 5]
```
volume     = 65 × 26 = 1690
totalReps  = 26,  avgReps = 6.5
repQuality = avg(1.0, 0.6, 0.6, 0.3) = 0.625
dropRate   = 1 - 5/8 = 0.375  → penalty = 0.10
```

Допустим `maxVolume = 2400`, `maxWeight = 65`:
```
Запись A:  0.45×(2400/2400) + 0.25×(60/65) + 0.30×1.0 − 0 = 0.45 + 0.231 + 0.30 = 0.981
Запись B:  0.45×(1690/2400) + 0.25×(65/65) + 0.30×0.625 − 0.10 = 0.317 + 0.25 + 0.188 − 0.10 = 0.655
```

**Результат:** Запись A лучше (0.981 > 0.655). Причина: "Объём −30%, повторы вышли из диапазона".

### Пример 2: Подтягивания (BW = 80 кг)

**Запись A:** BW × [10, 10, 9] → effectiveLoad = 80 кг
```
volume     = 80 × 29 = 2320
repQuality = avg(1.0, 1.0, 1.0) = 1.0
dropRate   = 1 - 9/10 = 0.10 → penalty = 0
```

**Запись B:** BW+10 кг × [2, 2, 2] → effectiveLoad = 90 кг
```
volume     = 90 × 6 = 540
repQuality = avg(0.3, 0.3, 0.3) = 0.3
dropRate   = 0 → penalty = 0
```

```
Запись A:  0.45×(2320/2320) + 0.25×(80/90) + 0.30×1.0 = 0.45 + 0.222 + 0.30 = 0.972
Запись B:  0.45×(540/2320) + 0.25×(90/90) + 0.30×0.3 = 0.105 + 0.25 + 0.09 = 0.445
```

**Результат:** BW × 10 гораздо лучше для гипертрофии.

**Запись C:** BW+10 кг × [8, 8, 7] → effectiveLoad = 90 кг
```
volume     = 90 × 23 = 2070
repQuality = avg(1.0, 1.0, 0.6) = 0.867
dropRate   = 1 - 7/8 = 0.125 → penalty = 0
```

```
Запись C:  0.45×(2070/2320) + 0.25×(90/90) + 0.30×0.867 = 0.401 + 0.25 + 0.260 = 0.911
```

**Результат:** +10 кг × [8,8,7] ~ конкурирует с BW × [10,10,9]. Это реальный прогресс!

---

## 6. Особые случаи

| Ситуация | Решение |
|----------|---------|
| Первая запись упражнения | `sessionScore` считается, но сравнение невозможно — показываем "Первая тренировка" |
| Одинаковый вес, +1 повтор | Объём чуть выше → небольшой плюс в delta |
| 1 подход | `fatiguePenalty = 0`, `repQuality` по одному подходу |
| Вес 0 (BW без доп. веса, `bodyWeight` не указан) | Использовать `weight = 0` как есть; предложить указать вес тела |
| Очень большой вес, 1 повтор | `repQuality` будет низким (0.3 для гипертрофии), что правильно |

---

## 7. Реализация по этапам

### Этап 1 — MVP (без миграций БД)
- Создать `WorkoutScoreCalculator` — utility-класс.
- Вход: `WorkoutEntry` + список последних N записей того же упражнения.
- Выход: `SessionScore` (число 0..1), `ComparisonResult` (лучше/хуже/без изменений), `reason` (строка).
- Коэффициенты: дефолт **гипертрофия / compound**.
- Показать на карточке журнала или на экране статистики: бейдж `▲▼→` + процент.

### Этап 2 — Персонализация
- Добавить поле `goal` в профиль пользователя (или в упражнение).
- Добавить `exerciseType` в таблицу `exercises` (compound/isolation).
- Добавить `bodyWeight` — либо в профиль (глобально), либо per-entry.
- Формула подбирает коэффициенты автоматически по `goal` + `exerciseType`.

### Этап 3 — Продвинутые функции
- RPE/RIR per set — корректировка `repQuality`.
- Авто-рекомендация веса/повторов на следующую тренировку.
- График `sessionScore` по времени.
- Персональные пороги на основе стажа тренировок.

---

## 8. Псевдокод (Kotlin)

```kotlin
data class SessionScore(
    val score: Double,           // 0.0 .. 1.0
    val volumeScore: Double,
    val intensityScore: Double,
    val repQuality: Double,
    val fatiguePenalty: Double
)

data class ComparisonResult(
    val status: Status,          // BETTER, SAME, WORSE
    val deltaPercent: Double,    // +6.4, -3.1
    val reason: String           // "Объём +12%, подходы в диапазоне"
)

enum class Status { BETTER, SAME, WORSE }

fun calcSessionScore(
    entry: WorkoutEntry,
    history: List<WorkoutEntry>,   // последние N записей этого упражнения
    targetRange: IntRange = 8..12, // зависит от goal
    wV: Double = 0.45,
    wI: Double = 0.25,
    wR: Double = 0.30
): SessionScore {
    val reps = entry.reps.split(",").map { it.trim().toInt() }
    val totalReps = reps.sum()
    val volume = entry.weight * totalReps

    val allVolumes = history.map { e ->
        val r = e.reps.split(",").map { it.trim().toInt() }
        e.weight * r.sum()
    } + volume
    val maxVolume = allVolumes.max()

    val allWeights = history.map { it.weight } + entry.weight
    val maxWeight = allWeights.max()

    val volumeScore = if (maxVolume > 0) volume / maxVolume else 0.0
    val intensityScore = if (maxWeight > 0) entry.weight / maxWeight else 0.0

    val repQuality = reps.map { r ->
        when (r) {
            in targetRange        -> 1.0
            in (targetRange.first - 2)..(targetRange.first - 1),
            in (targetRange.last + 1)..(targetRange.last + 3)
                                  -> 0.6
            else                  -> 0.3
        }
    }.average()

    val fatiguePenalty = if (reps.size >= 2) {
        val dropRate = 1.0 - reps.last().toDouble() / reps.first()
        when {
            dropRate <= 0.20 -> 0.00
            dropRate <= 0.35 -> 0.05
            dropRate <= 0.50 -> 0.10
            else             -> 0.15
        }
    } else 0.0

    val raw = wV * volumeScore + wI * intensityScore + wR * repQuality - fatiguePenalty
    val score = raw.coerceIn(0.0, 1.0)

    return SessionScore(score, volumeScore, intensityScore, repQuality, fatiguePenalty)
}

fun compare(current: SessionScore, previous: SessionScore): ComparisonResult {
    val delta = current.score - previous.score
    val pct = if (previous.score > 0) (delta / previous.score) * 100 else 0.0

    val status = when {
        delta >= 0.03  -> Status.BETTER
        delta <= -0.03 -> Status.WORSE
        else           -> Status.SAME
    }

    // Определяем главную причину
    val reasons = mutableListOf<String>()
    val volDelta = current.volumeScore - previous.volumeScore
    val intDelta = current.intensityScore - previous.intensityScore
    val repDelta = current.repQuality - previous.repQuality

    val maxComponent = maxOf(
        Math.abs(volDelta), Math.abs(intDelta), Math.abs(repDelta)
    )
    when (maxComponent) {
        Math.abs(volDelta) -> reasons += "Объём ${if (volDelta > 0) "↑" else "↓"}"
        Math.abs(intDelta) -> reasons += "Вес ${if (intDelta > 0) "↑" else "↓"}"
        Math.abs(repDelta) -> reasons += "Качество повторов ${if (repDelta > 0) "↑" else "↓"}"
    }
    if (current.repQuality < 0.6) {
        reasons += "повторы вне целевого диапазона"
    }

    return ComparisonResult(status, pct, reasons.joinToString(", "))
}
```

---

## 9. Что показать пользователю

Не показывать абстрактный «балл». Показывать **относительную оценку**:

- **Статус:** `▲ Лучше` / `→ Без изменений` / `▼ Хуже`
- **Процент:** `+6.4%` / `−3.1%`
- **Причина (1 строка):** "Объём ↑, подходы в диапазоне" или "+5 кг, но повторы вне 8–12"

Опционально: **Индекс формы (0..100)** — позиция текущей тренировки среди последних N. Значение 100 = лучший результат в окне; при новых записях окно сдвигается, индекс пересчитывается.

> Для максимальной простоты можно убрать число 0..100 и оставить только статус + процент + причину.
