# Анализ потенциальных проблем и технического долга

> Источник истины — реальный код в `app/src/main/java/com/example/gymprogress/`.
> Дата ревизии: 2026-04-25.
> Решения по исправлению — см. `docs/IMPROVEMENT_PLAN.md` (фаза 1 и 8).

## Что уже решено (для справки)

Эти проблемы из ранних ревизий **больше не актуальны** — фиксирую, чтобы не возвращаться:

- **Сортировка дат.** Дата хранится как ISO `yyyy-MM-dd` (`FormatUtils.STORAGE_DATE_PATTERN`). Сортировка `ORDER BY date ASC, id ASC` (`WorkoutDao`) корректна лексикографически и хронологически. Миграция со старого формата `dd.MM.yyyy` выполнена в `MIGRATION_5_6`. Перевод даты в `Long` не требуется.
- **Destructive migration.** В `AppDatabase.getDatabase()` `fallbackToDestructiveMigration` отсутствует, все миграции явные (`MIGRATION_2_3 … MIGRATION_5_6`).
- **Уникальность упражнений на уровне БД.** `Exercise` имеет `Index(value = ["name"], unique = true)` (миграция 3→4). Случай дублирующейся вставки невозможен в обход индекса.
- **Обработка ошибок БД и DataStore.** `WorkoutViewModel.safeDb` обёртывает все мутации; ошибка публикуется в `errorMessage`; `MainActivity` показывает её в Snackbar.
- **Список упражнений на «Прогрессе».** `StatsScreen` получает `allExercises` из `Exercise`-таблицы (`MainActivity:355-368`). Смешения с `workout_entries.exerciseName` нет.

---

## Актуальные проблемы

### 1. Нет внешнего ключа `WorkoutEntry → Exercise` (средний риск)

**Где:**
```
data/WorkoutEntry.kt
data/WorkoutDao.kt:36         renameExercise(oldName, newName)
viewmodel/WorkoutViewModel.kt:336  deleteExercise() — вызывает только exerciseDao.delete
data/FormatUtils.kt:87-134     normalizeExerciseNameKey, findExerciseByStoredName, …
```

`WorkoutEntry` хранит `exerciseName: String`. Связь между записью и упражнением — текстовая.

**Последствия:**
- Удаление упражнения через `exerciseDao.delete()` оставляет «висячие» записи в `workout_entries` со старым именем. Они продолжают участвовать в статистике и истории.
- Переименование частично пробрасывается через `WorkoutDao.renameExercise(oldName, newName)`, но опирается на точное совпадение строки. Любое расхождение в пробелах/регистре приведёт к разрыву связи.
- Из-за этого в коде накопились «костыли» нормализации имён: `FormatUtils.normalizeExerciseNameKey()`, `findExerciseByStoredName()`, `workoutEntryMatchesExercise()`.

**Рекомендация:**
- Ввести `WorkoutEntry.exerciseId: Long` с `@ForeignKey` на `Exercise.id`, заполнить миграцией через `LOWER(TRIM(exerciseName)) = LOWER(TRIM(exercises.name))`.
- Поведение при удалении — `onDelete = SET_NULL` или soft delete (`Exercise.isArchived: Boolean`), чтобы не терять историю.
- Эту правку полезно делать в одном релизе с фичей бэкапа (`IMPROVEMENT_PLAN.md` фаза 1), потому что для merge при импорте всё равно нужен стабильный `clientId`.

---

### 2. Уникальность упражнений case-sensitive и без UI-проверки ⚠️ устранено частично (2026-04-25)

**Что сделано:**
- `ExerciseDao.countByNormalizedName(name, excludeId)` — `LOWER(TRIM(REPLACE(name, char(160), ' ')))` на стороне SQL.
- `WorkoutViewModel.addExercise`/`updateExercise` проверяют `countByNormalizedName(normalizeExerciseNameKey(name))` до вставки и показывают понятное сообщение «Упражнение с таким именем уже есть» в Snackbar (через `errorMessage`).
- Покрывает разные регистры и неразрывные пробелы.

**Что осталось:**
- Жёсткая защита на уровне СУБД (индекс `COLLATE NOCASE` или денормализованная колонка `nameKey`) — отложено до фазы 8.3 в `IMPROVEMENT_PLAN.md`, чтобы все миграции схемы были в одном релизе.
- Маппинг `SQLiteConstraintException` в понятное сообщение пользователю (на случай race conditions, когда UI-проверка прошла, но за это время кто-то ещё вставил такое же имя).
- Множественные пробелы внутри строки (например «Жим  лёжа» с двумя пробелами) — Kotlin-нормализация их сжимает, SQL-проверка нет. Очень редкий случай.

---

### 3. Пустые правила Auto Backup ✅ устранено (2026-04-25)

`backup_rules.xml` и `data_extraction_rules.xml` заполнены явными `<include>` для БД Room (`gym_progress_db` + `-shm` / `-wal`) и DataStore (`files/datastore/`). В `data_extraction_rules.xml` присутствуют оба блока — `<cloud-backup>` и `<device-transfer>`.

Осталась ручная проверка на устройстве (cценарий «переустановил → данные восстановились»).

---

### 4. Мёртвый код в data-слое ✅ устранено (2026-04-25)

Удалены `WorkoutViewModel.exerciseNames` и `WorkoutDao.getAllExerciseNames()` — они нигде не использовались.

---

### 5. `ScoringSystem.ADVANCED` — текст ошибки в `_aiAdvice` (низкий риск)

**Где:**
```
viewmodel/WorkoutViewModel.kt:413-435  askAi() — при исключении кладёт «Ошибка: …» в _aiAdvice
```

Ошибка AI-вызова отображается **в том же поле**, что и нормальный совет. UI не различает «совет» и «ошибку», цвет/иконка одинаковые.

**Рекомендация:**
- Вынести `aiError: StateFlow<String?>` отдельно от `aiAdvice`.
- Показать в `TrainerScreen` ошибку в специальной плашке (`error`-цвет) с кнопкой «Повторить».
- Не блокирует, но улучшит UX.

---

### 6. Активная тренировка живёт только в Compose-стейте (средний риск)

**Где:**
```
ui/screens/ActiveWorkoutScreen.kt     все mutableState{...}, никакого ViewModel/Service
MainActivity.kt:255-274               showActiveWorkout overlay
```

Состояние активной тренировки (текущий подход, выполненные подходы, таймер отдыха) хранится только в `remember`/`rememberSaveable` внутри `ActiveWorkoutScreen`. Если Android убьёт процесс при низкой памяти — состояние теряется, тренировка обнуляется.

**Рекомендация:** см. `IMPROVEMENT_PLAN.md` фаза 6.2 — обернуть в `ForegroundService` с уведомлением и сохранением состояния.

---

## Сводная таблица приоритетов

| # | Проблема | Риск | Размер правки | Связь с планом |
|---|----------|------|---------------|----------------|
| 1 | Нет FK на `Exercise` | средний | M | фаза 8.3, в связке с 1 |
| 2 | Case-sensitive уникальность + нет UI-проверки ⚠️ частично | низкий | S | UI-проверка готова, миграция СУБД — фаза 8.3 |
| 3 | ~~Пустые правила Auto Backup~~ ✅ | — | — | устранено |
| 4 | ~~Мёртвый код `exerciseNames`~~ ✅ | — | — | устранено |
| 5 | AI-ошибка смешана с советом | низкий | S | новый пункт, можно добавить в фазу 2 |
| 6 | Активная тренировка теряется при kill | средний | M | фаза 6.2 |

---

## Чек-лист для ручного тестирования

- Удалить упражнение из каталога → проверить, что записи в журнале/истории остаются и помечены как-то предсказуемо (или предложен soft-delete после фазы 8.4).
- Создать упражнение «Жим лёжа», затем «жим лёжа» → пользователь должен получить понятное сообщение об ошибке, а не «UNIQUE constraint failed».
- На тестовом устройстве: установить, добавить записи, переустановить с включённым Google Backup → проверить восстановление.
- Запустить активную тренировку, свернуть, выгрузить из памяти через `adb shell am kill <pkg>` → оценить, что происходит с прогрессом сессии.
