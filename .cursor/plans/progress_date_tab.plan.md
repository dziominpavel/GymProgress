---
name: ""
overview: ""
todos: []
isProject: false
---

# Прогресс — вкладка «Дата»: итоговый план

**План обновлён после ответов на наводящие вопросы.**

## Ответы на вопросы (зафиксированы)

- **Вопрос 1 (что считать «предыдущим»):** Вариант **A** — предыдущее по времени выполнение этого же упражнения. «Первая тренировка» только если в истории нет более ранней записи по упражнению.
- **Вопрос 2 (блок «Итого»):** Показывать **только список упражнений с прогрессом**, без общего итога.

## Решения (из ответов)

- **1-A:** Сравнивать с **предыдущим выполнением этого же упражнения** (по времени). «Первая тренировка» только если в истории нет более ранней записи по упражнению.
- **2:** На вкладке «Дата» показывать **только список упражнений с прогрессом**, без блока «Итого».

---

## 1. WorkoutScoreCalculator.compareSessionByDate

**Файл:** [app/src/main/java/com/example/gymprogress/data/WorkoutScoreCalculator.kt](app/src/main/java/com/example/gymprogress/data/WorkoutScoreCalculator.kt)

- Для каждого упражнения на выбранной дате брать **prevEntry** не из «предыдущего календарного дня», а как **последнюю по времени запись этого упражнения до выбранной даты**:
  - `history = allEntries.filter { it.exerciseName == exName }`
  - Записи с `it.date < selectedDateStorage` (формат `yyyy-MM-dd`, сравнение строк корректно)
  - Отсортировать по `date` DESC, затем `id` DESC, взять первую — это `prevEntry`
- Вызов `compare(curEntry, prevEntry, history, goal, exType)` и формирование `ExerciseDayScore` оставить как есть.
- Общий блок «Итого» (overall, prevOverall, overallStatus, overallDeltaPercent, previousDate) для вкладки «Дата» не используется — можно не менять структуру `WorkoutDayReport` (она нужна для вкладки «Тренировка»), просто на UI не показывать карточку.

---

## 2. UI вкладки «Дата»: без блока «Итого»

**Вариант A (рекомендуемый):** параметр в `WorkoutDayReportView`, скрывать карточку «Итого», если режим «Дата».

- **StatsComponents.kt:** добавить параметр `showOverallCard: Boolean = true` в `WorkoutDayReportView`. Если `false` — не показывать `Card` с «Итого» и датой (блок с `displayName`, `Text("Итого")`, проценты), только `Column` с `report.exercises.forEach { ExerciseDayRow(ex) }`.
- **StatsScreen.kt:** при вызове для режима «Дата» передавать `WorkoutDayReportView(report, showOverallCard = false)`.

**Альтернатива:** завести отдельный Composable для режима «Дата», например `DateDayExercisesList(report: WorkoutDayReport)`, который рендерит только список `ExerciseDayRow`. Тогда не трогать сигнатуру `WorkoutDayReportView` (она используется во вкладке «Тренировка» с полной карточкой). Вызов из StatsScreen: `dateReport?.let { report -> item { DateDayExercisesList(report) } }` и внутри — только дата (опционально одна строка) + `report.exercises.forEach { ExerciseDayRow(ex) }`.

Рекомендация: параметр `showOverallCard` — меньше дублирования, один компонент.

---

## 3. Порядок списка и подпись «vs»

- В `ExerciseDayRow` уже выводится `ex.previousEntry` (и при раскрытии — детали). После смены логики `ex.previousEntry` будет запись с **другой** даты (предыдущее выполнение упражнения). Подпись «vs 05.03» в деталях будет корректной, если брать `ex.previousEntry?.date` и форматировать через `FormatUtils.formatDate`. Проверить, что в `ExerciseDayRow` при `ex.previousEntry != null` показывается дата предыдущей записи (при необходимости добавить отображение «vs [дата]» по `ex.previousEntry.date`).

---

## 4. Краткий чеклист реализации

1. **WorkoutScoreCalculator.kt** — в `compareSessionByDate` для каждого `exName`: `prevEntry` = последняя запись по `exName` с `date < selectedDateStorage` (сортировка по date desc, id desc), вместо `prevDay.filter { it.exerciseName == exName }.maxByOrNull { it.id }`.
2. **StatsComponents.kt** — `WorkoutDayReportView(report, showOverallCard: Boolean = true)`: если `!showOverallCard`, не рисовать Card с «Итого».
3. **StatsScreen.kt** — в режиме «Дата» вызывать `WorkoutDayReportView(report, showOverallCard = false)`.
4. При необходимости — в `ExerciseDayRow` явно показывать «vs [дата предыдущего выполнения]» из `ex.previousEntry?.date`.

После этого на вкладке «Дата» у каждого упражнения прогресс будет относительно предыдущего раза, когда его делали, и без общего блока «Итого».