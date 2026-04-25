# Reference: системы оценки тренировок

> Описание двух реализованных в коде систем скоринга и общего контракта `ScoringEngine`.
> Источник истины — `app/src/main/java/com/example/gymprogress/data/`:
> `ScoringEngine.kt`, `ScoringSystem.kt`, `SimplifiedScoreCalculator.kt`, `WorkoutScoreCalculator.kt`,
> `TrainingGoal.kt`, `ExerciseType.kt`.
> Этот документ описывает **актуальное поведение**, не план изменений.
> Дата ревизии: 2026-04-25.

## Зачем

Приложение хранит записи: упражнение, вес, массив повторов по подходам, дата.
По двум записям одного упражнения нужно понять, **лучше ли** новая тренировка.
Пользователь выбирает одну из двух систем оценки в настройках; обе реализуют общий интерфейс `ScoringEngine`.

---

## 1. Входные данные

### Запись (`WorkoutEntry`)
```
id           Long      autoincrement
date         String    "yyyy-MM-dd"
exerciseName String    например, "Жим лёжа"
weight       Double    рабочий вес в кг (для BW — уже effectiveWeight: bodyWeight + addedWeight)
reps         String    "10,8,8,7" (через запятую)
```

### Производные (вычисляются на лету)
```
sets       = reps.size
totalReps  = reps.sum()
volume     = weight × totalReps
```

### Параметры цели и типа упражнения
- `TrainingGoal` (`HYPERTROPHY` / `STRENGTH` / `ENDURANCE`) — глобальная настройка пользователя.
- `ExerciseType` (`COMPOUND` / `ISOLATION`) — поле упражнения.

#### `TrainingGoal` (`data/TrainingGoal.kt`)
| Цель | targetRange | nearRange | farRange |
|------|-------------|-----------|----------|
| Гипертрофия | 8–12 | 6–15 | 1–30 |
| Сила | 3–6 | 1–8 | 1–30 |
| Выносливость | 15–25 | 12–30 | 1–40 |

`farRange` сейчас не используется в расчётах, но закладывается под расширения.

### Bodyweight-упражнения
Для упражнений с `Exercise.isBodyweight = true` пользователь вводит **уже** общий вес `bodyWeight + addedWeight` (см. `AddEntryDialog`). В калькуляторах `entry.weight` используется как есть; параметр `bodyWeightKg` нужен только как gate: если он `null`, для BW-упражнения `effectiveWeight = 0` → `E1RM = 0`, чтобы не считать «фантомный» прогресс.

---

## 2. Общий контракт `ScoringEngine`

`data/ScoringEngine.kt`:

```kotlin
interface ScoringEngine {
    fun calcSessionScore(
        entry: WorkoutEntry,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): SessionScore

    fun compare(
        current: WorkoutEntry,
        previous: WorkoutEntry?,
        history: List<WorkoutEntry>,
        goal: TrainingGoal,
        exerciseType: ExerciseType,
        bodyWeightKg: Double?,
        isBodyweightExercise: Boolean
    ): ComparisonResult

    fun compareDays(...): WorkoutDayReport?
    fun compareSessionByDate(...): WorkoutDayReport?
}
```

Реализации:
- `SimplifiedScoreCalculator` (`object`) — **упрощённая** система.
- `WorkoutScoreCalculator` (`object`) — **усложнённая** (advanced) система.

`WorkoutViewModel.scoringEngine` собирается из выбранного `ScoringSystem`:
```kotlin
when (system) {
    ScoringSystem.SIMPLIFIED -> SimplifiedScoreCalculator
    ScoringSystem.ADVANCED  -> WorkoutScoreCalculator
}
```

### Общие data-классы (`WorkoutScoreCalculator.kt`)

```kotlin
enum class ProgressStatus { BETTER, SAME, WORSE, FIRST }

enum class ProgressMetricType(val displayName: String) {
    VOLUME("Объём"),
    E1RM("E1RM"),
    TOTAL_REPS("Повторы"),
    STIMULUS("Стимул")
}

data class SessionScore(
    val score: Int,                  // 0..1000
    val rawMetric: Double,           // объём / E1RM / стимул в зависимости от системы и цели
    val metricType: ProgressMetricType,
    val repQuality: Double,          // 0..1
    val fatiguePenalty: Double,      // 0..0.10
    val components: ScoreComponents
)

data class ComparisonResult(
    val status: ProgressStatus,
    val deltaPercent: Double,        // ±%
    val reason: String,              // короткий человекочитаемый ярлык
    val details: ScoreDetail?        // развёрнутые детали для UI «подробнее»
)
```

### Утилита `selectBestSessionEntry`
В `ScoringEngine.kt` есть free-функция `selectBestSessionEntry(...)`, которая выбирает «лучшую» запись из набора:
- `SIMPLIFIED` — по `rawMetric` (E1RM); ties — позже по дате, затем по `id`.
- `ADVANCED` — по `score` (composite); ties — позже по дате, затем по `id`.

Используется во вкладке «Прогресс» и в диалоге «Новая запись», чтобы оба места показывали один и тот же рекорд.

---

## 3. Упрощённая система — `SimplifiedScoreCalculator`

**Главная метрика:** оценочный одноразовый максимум (E1RM) в кг.
**Цель:** интуитивная единица «сколько ты выжал бы на раз», понятная без объяснений.
**Цель `goal` и тип `exerciseType` фактически не используются** — модель универсальная.

### 3.1 E1RM по одному подходу — гибрид Epley + Brzycki

```
если reps == 1                  → E1RM = weight
если reps ≤ 10                  → E1RM = weight × (1 + reps / 30)        // Epley
если 11 ≤ reps ≤ 15             → E1RM = weight × 36 / (37 − reps)       // Brzycki
если reps > 15                  → reps капается на 15 (Brzycki)
если reps > 30                  → reps капается на 30 до выбора формулы
```

На границе `reps = 10` обе формулы дают `× 1.333` — переход без скачка.
Epley точнее на 1–10 повторов, Brzycki — на 11–15.

### 3.2 Лестница усилия (Effort Ladder)

Введена для устранения завышения от ранних подходов. Допущение: только последний подход близок к отказу. RIR пользователь не вводит.

```
positionFromEnd = sets - 1 - i
коэффициент по позиции (STANDARD_EFFORT):
  0 (последний)        1.00
  1 (предпоследний)    0.97
  2 (третий с конца)   0.94
  3+ (ранние подходы)  0.91

adjusted_E1RM[i] = raw_E1RM[i] × коэффициент
```

`EffortProfile` вынесен как data class — в будущем под него можно подложить альтернативные стили (обратная пирамида и т.п.), но сейчас в коде только `STANDARD_EFFORT`.

### 3.3 Бонус за подтверждение

Решает кейс «1×5 vs 5×5»: повторяемый результат должен оцениваться выше одиночного.

```
bestRaw = max(raw_E1RM)
confirmingSets = count { raw_E1RM[i] ≥ bestRaw × 0.95 }
extraConfirming = max(0, confirmingSets − 1)

volumeBonus = min(0.05, 0.015 × extraConfirming)   // до +5%
```

**Важно:** считается по **raw** (без лестницы). Иначе ранние тяжёлые подходы фильтровались бы лестницей и не «подтверждали» рекорд.

### 3.4 Умный штраф за усталость

```
dropRate = 1 − reps.last / reps.first

fatiguePenalty:
  dropRate ≤ 0.20  → 0.00
  dropRate ≤ 0.35  → 0.03
  dropRate ≤ 0.50  → 0.06
  иначе            → 0.10
```

Применяется только если **лучший подход не последний** (`bestIndex < lastIndex`):
```
smartPenalty = if (bestIndex < lastIndex) fatiguePenalty × 0.5 else 0
```

Логика: «лучший в начале + развал к концу» означает, что первый подход был ближе к отказу, чем считает формула, → E1RM слегка завышен.

### 3.5 Итог

```
final_E1RM = best × (1 + volumeBonus) × (1 − smartPenalty)
```

`SessionScore.rawMetric = final_E1RM`, `metricType = E1RM`.

`SessionScore.score` — относительная позиция:
```
bestE1RM_history = max E1RM по истории, исключая текущую запись
score = (current_E1RM / max(bestE1RM_history, current_E1RM)) × 100
score ∈ [0..1000] (cap для возможных PR)
```

### 3.6 Сравнение

База — **среднее E1RM за последние `TREND_SIZE = 3` записи** (без текущей).
```
deltaPercent = (current_E1RM − baseline_E1RM) / baseline_E1RM × 100

статус:
  deltaPercent ≥ +5%    → BETTER
  deltaPercent ≤ −5%    → WORSE
  иначе                 → SAME
```

`reason` — короткая строка вида «1RM 95.32 кг ↑, +2.50 кг» или «1RM стабилен».

### 3.7 Краевые случаи

- **Один подход:** `confirmingSets = 1`, `volumeBonus = 0`, `fatiguePenalty = 0`.
- **Пустые reps или вес ≤ 0:** `E1RM = 0`, `score = 0`.
- **BW без указанного `bodyWeightKg`:** `E1RM = 0` (UI показывает подсказку «укажите вес тела» через `isAnthropometryComplete`).
- **Очень много повторов (>30):** капается на 30, дополнительно на 15 для Brzycki.
- **Несколько записей одного упражнения за день:** каждая оценивается отдельно; в качестве «лучшей за день» берётся та, что вернёт `selectBestSessionEntry`.

---

## 4. Усложнённая система — `WorkoutScoreCalculator`

**Главная метрика зависит от цели:**
- `HYPERTROPHY` → составной «стимул» (см. ниже).
- `STRENGTH` → E1RM по лучшему подходу (классическая Epley/Brzycki без лестницы).
- `ENDURANCE` → объём (`weight × totalReps`).

**Шкала score:** `0..1000`, где `100 ≈ личный рекорд`. >100 = новый рекорд.

### 4.1 Гипертрофия — composite stimulus

#### Зональный коэффициент (продуктивные диапазоны)

```
COMPOUND:
  reps  5..10   → 1.00
  reps 11..15   → 0.95
  reps  3..4    → 0.75
  reps 16..20   → 0.80
  иначе         → 0.50

ISOLATION:
  reps  8..15   → 1.00
  reps  6..7    → 0.90
  reps 16..20   → 0.95
  reps 21..30   → 0.75
  иначе         → 0.50
```

#### Компоненты

```
tension      = currentWeight / bestHistoryWeight                    // напряжение
stimulus     = Σ(reps × zoneCoefficient)                            // продуктивный объём
productive   = √(currentStimulus / bestHistoryStimulus)             // saturation
repQuality   = average(zoneCoefficient по подходам)
```

`√` смягчает рост `productive` после высокого пика — линейное отношение наказывало бы за чуть меньший объём после рекордного.

#### Веса по типу упражнения

| ExerciseType | wT (tension) | wP (productive) | wR (repQuality) |
|--------------|--------------|------------------|------------------|
| COMPOUND | 0.55 | 0.25 | 0.20 |
| ISOLATION | 0.30 | 0.45 | 0.25 |

Базовые упражнения сильнее «видят» тяжёлый вес, изоляции — продуктивный объём.

#### Итоговый балл

```
compositeRaw = wT × tension + wP × productive + wR × repQuality
rawMetric    = compositeRaw × 100
fatiguePenalty = (та же формула, что в Simplified, см. 3.4)
score        = (rawMetric − fatiguePenalty × 10).coerceIn(0, 1000)
```

`metricType = STIMULUS`.

#### Guardrail (anti-WORSE)

Если одновременно:
- `currentWeight ≥ 1.05 × среднее за последние 3`,
- все подходы попали в продуктивную зону (`zoneCoefficient ≥ 0.75`),
- `totalReps` не упал больше чем на 30%,
- `fatiguePenalty` не вырос больше чем на 0.03,

тогда статус **не может быть WORSE** — принудительно `SAME` с причиной «Тяжёлая работа в продуктивной зоне».

**Исключение:** сверхтяжёлые синглы/дабллы (`all reps ≤ 3`) — это не гипертрофийный прогресс, guardrail не срабатывает.

### 4.2 Сила (`STRENGTH`)

```
E1RM = repMaxByBestSet (Epley до 10 повторов, Brzycki выше)
metric = E1RM
yourBest = max(E1RM в истории)
rawScore = (metric / yourBest) × 100
score    = (rawScore − fatiguePenalty × 10).coerceIn(0, 1000)
```

Лестница усилия и бонус за подтверждение **не применяются** — это упрощённая legacy-формула. `metricType = E1RM`.

### 4.3 Выносливость (`ENDURANCE`)

```
metric = volume = weight × totalReps
yourBest = max(volume в истории)
rawScore = (metric / yourBest) × 100
score    = (rawScore − fatiguePenalty × 10).coerceIn(0, 1000)
```

`metricType = VOLUME`.

### 4.4 Сравнение

База — среднее за последние `TREND_SIZE = 3` записи.
- Hypertrophy: дельта по `compositeRaw` (= `rawMetric`).
- Strength / Endurance: дельта по `metric` (`E1RM` или `volume` соответственно).
- Порог: ±5% (`PROGRESS_THRESHOLD_PCT`).

`reason` — детализированная строка из компонентов: «Напряжение ↑, Продуктивные повторы ↑», «Объём −12%, повторы вне зоны» и т.п.

### 4.5 Отчёт по дню (`compareDays`, `compareSessionByDate`)

Возвращает `WorkoutDayReport` для выбранной мышечной группы или даты:
- список упражнений в дне с индивидуальным `ExerciseDayScore` (current/previous + delta);
- общий `overallScore` — среднее по упражнениям;
- общий `overallStatus` — по тому же порогу ±5%.

Используется на вкладке «Прогресс» в режимах «Группа мышц» и «Дата».

---

## 5. Краевые случаи и инварианты (общие)

| Ситуация | Поведение |
|----------|-----------|
| Первая запись упражнения (нет `previous`) | `status = FIRST`, `deltaPercent = 0`, `reason = "Первая тренировка"` |
| Меньше 3 предыдущих записей | Базой служит то, что есть; если совсем пусто — `FIRST` с пометкой «Мало данных для сравнения» (Simplified) |
| Один подход в записи | `fatiguePenalty = 0`, `volumeBonus = 0` |
| Очень большой вес и `reps = 1` | В Simplified — высокий E1RM по последнему. В Hypertrophy — `repQuality = 0.5`, guardrail не срабатывает |
| Запись с пустым/некорректным `reps` | `parseReps()` отфильтрует нули и не-числа; пустой результат → нулевой score |
| Несколько записей за один день | Каждая оценивается отдельно; «лучшая за день» — `selectBestSessionEntry` |

Сортировка истории при выборе trend-окна — **по убыванию** даты и `id` (это совпадает с правилом `date ASC, id ASC` для UI: при `take(N)` по убыванию возьмутся последние N записей).

---

## 6. Что показывается пользователю

UI избегает «сырого» числа в качестве главного показателя.

- **Статус:** `▲ Лучше / → Без изменений / ▼ Хуже / ⚑ Первая`.
- **Процент:** `+6.4%` / `−3.1%`.
- **Причина:** короткая строка из `ComparisonResult.reason`.
- **Главная метрика:**
  - Simplified — оценочный 1RM в кг (`90.50 кг`).
  - Advanced + Hypertrophy — индекс стимула (`102 / 100 ≈ рекорд`).
  - Advanced + Strength — оценочный 1RM в кг.
  - Advanced + Endurance — объём в кг·повт.
- **Подробности** (на отдельном экране/диалоге): компоненты (`tension`, `productive`, `repQuality`) и их предыдущие значения, сравнение веса/объёма/повторов.

`StatsHelp.kt` содержит пользовательское объяснение обеих систем.

---

## 7. Какие места в UI читают эти расчёты

- `StatsScreen` + `StatsComponents` — вкладка «Прогресс»: график, рекорд, day report.
- `ExerciseProgressChartScreen` — график E1RM/score по дням.
- `AddEntryDialog` — лучшая запись по упражнению (через `selectBestSessionEntry`), используется для подсказки и для сравнения PR.
- `JournalScreen` — статус последней записи (через `compare`).
- `WorkoutHistoryScreen` — то же по каждой записи.
- `TrainerScreen` / `TrainerRecommendationEngine` — использует историю для подбора веса/повторов на следующую тренировку (отдельная логика, не часть scoring).

---

## 8. Расширяемость

Закладки на будущее, видимые в коде:
- `EffortProfile` (Simplified) — позволит подключить альтернативные стили сессии (обратная пирамида и т.п.) через выбор пользователя.
- `farRange` в `TrainingGoal` — пока не используется, но зарезервирован под более мягкую градацию repQuality.
- `ScoringEngine`-интерфейс — точка добавления третьей системы оценки без правок UI.
- `ProgressMetricType.TOTAL_REPS` объявлен в enum, но текущие калькуляторы в качестве метрики выносливости используют `VOLUME`. Если понадобится — переключение метрики при `ENDURANCE` тривиально.

Когда что-то из этого будет реализовываться, пункт нужно перенести из «расширяемости» в раздел соответствующей системы.
