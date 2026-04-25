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

### 2. Уникальность упражнений case-sensitive и без UI-проверки (низкий-средний риск)

**Где:**
```
data/Exercise.kt:7-10                  Index(value = ["name"], unique = true)
data/ExerciseDao.kt:34-35              countByName(name) — есть, но не вызывается
viewmodel/WorkoutViewModel.kt:308-324  addExercise() — без проверки countByName
```

**Последствия:**
- Индекс case-sensitive: «Жим лёжа», «жим лёжа» и «Жим лежа» с другим пробелом проходят как разные упражнения.
- В UI перед вставкой проверка не выполняется. При попадании на duplicate возникает `SQLiteConstraintException`, которую `safeDb` показывает как «UNIQUE constraint failed: exercises.name» — нечитаемо для пользователя.
- В коде уже есть `FormatUtils.normalizeExerciseNameKey()` и `countByName()` — нужна только связка их вместе.

**Рекомендация:**
- Сделать индекс `COLLATE NOCASE` (миграция: пересоздать таблицу/индекс) **или** добавить поле `nameKey: String` (`normalizeExerciseNameKey(name)`) и индекс UNIQUE на нём.
- В `ExercisesScreen` перед вставкой/обновлением вызывать `exerciseDao.countByName(normalizedName)` и показывать понятное сообщение «Упражнение с таким именем уже есть».
- В `safeDb` при ловле `SQLiteConstraintException` для таблицы `exercises` подставлять понятный текст.

---

### 3. Пустые правила Auto Backup (низкий риск, сейчас работает на defaults)

**Где:**
```
AndroidManifest.xml                   android:allowBackup="true", ссылки на оба XML
res/xml/backup_rules.xml              <full-backup-content/> без include/exclude
res/xml/data_extraction_rules.xml     <cloud-backup/>, <device-transfer/> закомментированы
```

**Последствия:**
- Auto Backup сейчас работает с дефолтным поведением Android (бэкапит большую часть `data/` приложения), но это не задокументировано явно и может перестать включать БД при будущих изменениях.
- Нет тестового сценария «переустановил → данные восстановились».

**Рекомендация:** см. `IMPROVEMENT_PLAN.md` фаза 1.1 — заполнить `<include>`-блоки для БД (`gym_progress_db`) и DataStore (`settings.preferences_pb`), проверить на тестовом устройстве.

---

### 4. Мёртвый код в data-слое (низкий риск, чистка)

**Где:**
```
data/WorkoutDao.kt:30-31              getAllExerciseNames(): Flow<List<String>>
viewmodel/WorkoutViewModel.kt:81-82   exerciseNames: StateFlow<List<String>>
```

`WorkoutViewModel.exerciseNames` объявлен как публичный `StateFlow`, но не используется ни в одном `@Composable`. Соответствующий DAO-метод также никем не вызывается.

**Рекомендация:** удалить оба символа вместе с импортами. Уменьшит количество подписок на Room и упростит ViewModel.

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
| 2 | Case-sensitive уникальность + нет UI-проверки | низкий-средний | S | фаза 8.2 |
| 3 | Пустые правила Auto Backup | низкий | XS | фаза 1.1 |
| 4 | Мёртвый код `exerciseNames` | низкий | XS | фаза 8.1 |
| 5 | AI-ошибка смешана с советом | низкий | S | новый пункт, можно добавить в фазу 2 |
| 6 | Активная тренировка теряется при kill | средний | M | фаза 6.2 |

---

## Чек-лист для ручного тестирования

- Удалить упражнение из каталога → проверить, что записи в журнале/истории остаются и помечены как-то предсказуемо (или предложен soft-delete после фазы 8.4).
- Создать упражнение «Жим лёжа», затем «жим лёжа» → пользователь должен получить понятное сообщение об ошибке, а не «UNIQUE constraint failed».
- На тестовом устройстве: установить, добавить записи, переустановить с включённым Google Backup → проверить восстановление.
- Запустить активную тренировку, свернуть, выгрузить из памяти через `adb shell am kill <pkg>` → оценить, что происходит с прогрессом сессии.
