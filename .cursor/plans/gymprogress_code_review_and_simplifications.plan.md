---
name: "GymProgress code review and simplifications "
overview: "Актуальное ревью (апрель 2026): читаемость, отсутствие мёртвого кода, эффективность и надёжность. Сверка с уже выполненными шагами и приоритетный план рефакторинга. "
todos: []
isProject: true
---

# Ревью GymProgress: простота, эффективность, надёжность

## Сводка

Проект уже выстроен разумно: один `WorkoutViewModel`, Room + DataStore, интерфейс `ScoringEngine` с двумя реализациями, часть тяжёлых экранов вынесена в отдельные файлы. Основные точки роста: **мёртвый и «заглушечный» API**, **дублирование и размер крупных файлов**, **пакетные операции БД**, **упрощение `GymProgressApp`**, **согласованность дефолтов и сортировок**.

---

## Уже сделано (относительно старых планов)

- [FormatUtils.kt](app/src/main/java/com/example/gymprogress/data/FormatUtils.kt): константы `STORAGE_DATE_PATTERN` / `DISPLAY_DATE_PATTERN`, `toStorageDate`, `parseStorageDate` (в т.ч. fallback на `dd.MM.yyyy`).
- [EditEntryDialog.kt](app/src/main/java/com/example/gymprogress/ui/screens/EditEntryDialog.kt) вынесен из журнала; используется из `JournalScreen` и `WorkoutHistoryScreen`.
- Прогресс разбит: [StatsHelp.kt](app/src/main/java/com/example/gymprogress/ui/screens/StatsHelp.kt), [StatsComponents.kt](app/src/main/java/com/example/gymprogress/ui/screens/StatsComponents.kt); [StatsScreen.kt](app/src/main/java/com/example/gymprogress/ui/screens/StatsScreen.kt) ~500 строк.
- [AppNavigationScaffold.kt](app/src/main/java/com/example/gymprogress/ui/navigation/AppNavigationScaffold.kt) — табы и меню «Ещё» не в одной простыне с логикой экранов.
- `saveCompletedWorkout` и обработка ошибок БД: [WorkoutViewModel.kt](app/src/main/java/com/example/gymprogress/viewmodel/WorkoutViewModel.kt) (`errorMessage`, Snackbar в [MainActivity.kt](app/src/main/java/com/example/gymprogress/MainActivity.kt)).

---

## Мёртвый и лишний код (убрать или задействовать)

| Что | Где | Действие |
|-----|-----|----------|
| `previousSameDaySession`, `previousSameDaySessionDate` | `WorkoutViewModel` — алиасы на `previousSessionForJournal` / `previousSessionDateForJournal` | Нигде не собираются в UI → **удалить** или оставить один комментарий «deprecated alias», если есть внешние потребители (сейчас нет). |
| `getAlternatives` | `WorkoutViewModel` → `TrainerRecommendationEngine` | UI не вызывает → **удалить из ViewModel**; при появлении UI «альтернативы упражнения» вызывать движок напрямую или вернуть метод. |
| `clearAiAdvice` | `WorkoutViewModel` | Не вызывается → **удалить** или вызывать при уходе с экрана Тренера / новом запросе (если нужна очистка состояния). |
| Параметр `exercises` в `WorkoutSummary` | [ActiveWorkoutScreen.kt](app/src/main/java/com/example/gymprogress/ui/screens/ActiveWorkoutScreen.kt) (`@Suppress("unused")`) | **Удалить параметр** и аргумент вызова. |
| `historyNameHint`, `allExercises` в `entriesForExerciseNames` | [TrainerRecommendationEngine.kt](app/src/main/java/com/example/gymprogress/data/TrainerRecommendationEngine.kt) | Помечены `UNUSED_PARAMETER` → **убрать из сигнатуры** и обновить вызовы, либо реализовать задуманное сопоставление имён (см. `docs/POTENTIAL_ERRORS_ANALYSIS.md`). |
| `ExampleUnitTest` / шаблон instrumented-теста | `app/src/test`, `androidTest` | Не дают ценности → **заменить** минимальным тестом домена (например, уже есть [SimplifiedScoreCalculatorBestEntryTest.kt](app/src/test/java/com/example/gymprogress/data/SimplifiedScoreCalculatorBestEntryTest.kt)) или удалить шаблон. |

---

## Читаемость и структура

1. **`GymProgressApp` ([MainActivity.kt](app/src/main/java/com/example/gymprogress/MainActivity.kt))**  
   Много `rememberSaveable` / `collectAsState` и цепочка `if (overlay) { … return }`. Упростить:
   - сгруппировать флаги в `data class AppOverlayState` или sealed-класс «текущий полноэкранный экран» (взаимоисключающие экраны — один тип вместо семи boolean);
   - вынести «разрешение» активной тренировки (`activeWorkoutRec` + `workoutRecommendation`) в чистую функцию или `LaunchedEffect`, без присваивания в теле Composable.

2. **`WorkoutViewModel`** (~515 строк)  
   Повторяющийся шаблон `viewModelScope.launch { try { … } catch { _errorMessage … Log.e } }` для CRUD и настроек → **вспомогательная** `inline fun safeDb(block: suspend () -> Unit)` или делегат, чтобы тело методов оставалось одной строкой вызова DAO/репозитория.

3. **`TrainerRecommendationEngine`** (~716 строк)  
   Логично разнести по файлам в том же пакете (например, `TrainerSplitDay.kt`, `TrainerDeload.kt`, `TrainerExerciseList.kt`) с `internal` функциями и одним публичным фасадом-классом — **без изменения формул**, только границы файлов и имена.

4. **`StatsScreen` дефолты**  
   `scoringEngine: ScoringEngine = WorkoutScoreCalculator` при `scoringSystem: ScoringSystem = ScoringSystem.SIMPLIFIED` вводит в заблуждение в превью/тестах. **Согласовать дефолты** (например, `SimplifiedScoreCalculator` + `SIMPLIFIED`).

5. **Локальные `DateTimeFormatter` в UI**  
   В [JournalScreen.kt](app/src/main/java/com/example/gymprogress/ui/screens/JournalScreen.kt) остаются паттерны `"d MMMM"`, `"EEEE"` — опционально вынести в `FormatUtils` с `Locale` для единообразия.

---

## Эффективность и надёжность

1. **`saveCompletedWorkout`**  
   Для каждой группы вызывается `addEntry`, а тот делает **отдельный** `launch` + `insert`. При нескольких упражнениях/весах — много параллельных транзакций и неатомарность «одна тренировка».  
   **План:** собрать список `WorkoutEntry` и вставить в одной `@Transaction` в DAO (`insertAll`) или одном `runBlocking`/`withContext` с одной корутиной и последовательными insert в рамках транзакции Room.

2. **`getAllEntries()`: `ORDER BY date DESC`** ([WorkoutDao.kt](app/src/main/java/com/example/gymprogress/data/WorkoutDao.kt))  
   В [AGENTS.md](../../AGENTS.md) зафиксирован порядок **старые сверху** (`date ASC`, `id ASC`). Сейчас экраны частично **пересортировывают** сами (например, журнал — `sortedTodayEntries`).  
   **План:** проверить все потребители `allEntries`; привести к одному контракту (либо менять запрос + поправить места, где нужен обратный порядок, либо явно документировать «DAO отдаёт DESC, UI нормализует»).

3. **Дублирование `combine` в ViewModel**  
   `journalSessionState` и `previousSessionDayMuscleGroups` оба тянут `trainerSettings`, `allEntries`, `allExercises`, `_journalSelectedDayIndex` — часть вычислений дня сплита повторяется. **План:** один производный `StateFlow` с data class сессии журнала + производные `map`, чтобы реже вызывать `findNextDaySessionInSplit` / `getNextDayIndex`.

4. **Самоимпорт в [SimplifiedScoreCalculator.kt](app/src/main/java/com/example/gymprogress/data/SimplifiedScoreCalculator.kt)**  
   Строка `import …SimplifiedScoreCalculator.calcE1RMForEntry` выглядит как артефакт — **убрать**, вызывать метод объекта без лишнего импорта.

---

## Фазы рефакторинга (обновлённый порядок)

### Фаза A — низкий риск, быстрый выигрыш

- Удалить неиспользуемые API и параметры (таблица «Мёртвый код»).
- Согласовать дефолты `StatsScreen` / `StatsComponents` для превью.
- Почистить `SimplifiedScoreCalculator` import.

### Фаза B — читаемость без смены поведения

- `AppOverlayState` или sealed «fullscreen» в `GymProgressApp`.
- `safeDb` / общий обработчик ошибок в `WorkoutViewModel`.
- Опционально: русские форматы дат из `JournalScreen` → `FormatUtils`.

### Фаза C — надёжность и производительность

- Транзакционное сохранение завершённой тренировки.
- Ревизия сортировки `getAllEntries` и всех списков (согласование с AGENTS.md).
- Объединение дублирующих `combine` для журнала.

### Фаза D — по желанию

- Разбиение `TrainerRecommendationEngine` на несколько файлов.
- Слой `WorkoutRepository` / `ExerciseRepository` для тестов и единой точки доступа к БД.
- Миграция дат на `Long` (epoch day) — только при отдельном ТЗ и тщательной миграции (см. [POTENTIAL_ERRORS_ANALYSIS.md](../../docs/POTENTIAL_ERRORS_ANALYSIS.md)).

---

## Что не ломать без явной цели

- Публичные контракты экранов и алгоритмы скоринга / тренера.
- Схему Room без новой миграции и проверки данных.
- Единый `WorkoutViewModel` как источник правды для экранов.

---

## Краткая таблица

| Проблема | Решение | Риск |
|----------|---------|------|
| Неиспользуемые поля/методы ViewModel и параметры Composable | Удалить или подключить в UI | Низкий |
| Много отдельных `insert` после тренировки | Транзакция / batch insert | Низкий при тестах |
| Разный порядок дат DAO vs правила AGENTS | Единый контракт + правки сортировки | Средний — регрессия UI |
| Огромный `TrainerRecommendationEngine` | Несколько файлов, тот же API | Низкий |
| Дублирование catch в ViewModel | `safeDb` helper | Низкий |
| Перегруз `GymProgressApp` флагами | Sealed / data class навигации | Низкий |
