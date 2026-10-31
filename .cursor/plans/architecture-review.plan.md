# Архитектурное ревью GymProgress

> Источник истины по плану. Основано на ревью кода от 2026-10-31.
> Цель: убрать потери состояния, снизить дублирование и нагрузку на composition,
> не ломая правило проекта «один `WorkoutViewModel` + ручная навигация».

## Вердикт

Архитектура рабочая для текущего масштаба (один модуль, ~14.5k строк Kotlin).
Не требуется переходить на multi-module, Hilt или Navigation Compose.
Основные проблемы: владение состоянием, дублирование правил и вычисления в composition.

## P0 — стабильность

- [x] Активная тренировка переживает поворот экрана и пересоздание Activity.
      Состояние вынесено в `ActiveWorkoutUiState`, сохраняется через `rememberSaveable`
      и JSON-схему `ActiveWorkoutStateDto`; экран стал controlled-composable.
- [x] Системный «Назад» в настройках сохраняет вес/рост, а не молча теряет черновик.
- [x] Undo-delete в журнале сразу скрывает запись (устранён устаревший `remember`).
- [x] Отчёты статистики пересчитываются при смене веса тела (`bodyWeightKg` в ключах).
- [x] Android 14+: разрешение `ACTIVITY_RECOGNITION` для FGS типа health и
      защита `startForeground` от `SecurityException` (иначе процесс падал).

## P1 — производительность и целостность

- [ ] Убрать O(N²) в `StatsScreen` и `ExerciseProgressChart`.
- [ ] Сгруппировать состояние: `JournalUiState`/`StatsUiState`, убрать 30 `collectAsState`.
- [ ] `TrainerRecommendationEngine`: один проход по истории, реализовать или убрать `priorityGroups`.
- [ ] Merge: не дублировать «сиротские» записи (identity + natural key).
- [ ] Не глотать `CancellationException` в `safeDb`, backup и worker.
- [ ] Backup: `associateBy` в replace, bounded read, кодирование вне main thread.

## P2 — сокращение кода

- [ ] Вынести `E1rm`, `WorkoutEntry.repList`, `DayReportBuilder`; убрать дубли скоринга.
- [ ] Общие UI-компоненты: `ScreenHeader`, `SelectableCard`, `rememberUndoDelete`,
      `WorkoutEntryCard`, формы Add/Edit.
- [ ] `SettingsRepository`: один `settingsSnapshot` flow + enum-хелперы.
- [ ] Разгрузить `WorkoutViewModel` и `MainActivity` через чистые калькуляторы.
- [ ] Удалить мёртвый код и неиспользуемые параметры.
- [ ] Characterization-тесты на скоринг и тренер; тесты rotation/back/undo.

## Проверка

- `:app:testDebugUnitTest` — успешно, включая round-trip `ActiveWorkoutStateDto`.
- `:app:connectedDebugAndroidTest` — 12/12.
- `:app:lintDebug`, `:app:assembleDebug` — успешно.
- вручную: поворот экрана во время тренировки сохраняет подход, таймер и прогресс;
  системный back в настройках сохраняет вес; undo-delete скрывает запись сразу;
  запуск тренировки на Android 16 не падает из-за FGS типа health.

## Статус

- P0 закрыт 2026-10-31.
- Спеки актуализированы: `active-workout` (3 требования), `app-settings`
  (1 требование), `progress-statistics` (1 требование), `workout-logging`
  дополнена удалением с отменой.
- `openspec validate --all --strict` — 7/7.
- Следующий шаг — P1: O(N²), группировка состояния, trainer engine.
