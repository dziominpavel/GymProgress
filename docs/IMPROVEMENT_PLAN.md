# План улучшений GymProgress

> Статус: **план, без реализации**.
> Источник истины — только реальный код в `app/src/main/java/com/example/gymprogress/...`.
> Прочая документация в `docs/` будет отрефакторена позднее отдельной задачей и здесь не цитируется.
> Дата составления: 2026-04-25.

---

## Текущее состояние (по факту кода)

Чтобы план не повторял уже сделанное, фиксирую baseline:

- **Дата записей** хранится как `String` в формате ISO `yyyy-MM-dd` (`FormatUtils.STORAGE_DATE_PATTERN`). Сортировка `ORDER BY date ASC, id ASC` корректна. Миграция со старого формата `dd.MM.yyyy` уже выполнена в `MIGRATION_5_6`. **Перевод даты в `Long` не нужен.**
- **БД** — Room, версия 6, явные миграции `MIGRATION_2_3 … MIGRATION_5_6`, `fallbackToDestructiveMigration` отсутствует.
- **Уникальный индекс** `Index(value = ["name"], unique = true)` на `Exercise.name` есть (миграция 3→4). Однако индекс **case-sensitive**, и UI до вставки уникальность не проверяет — опираемся на `SQLiteConstraintException` через `safeDb`.
- **Связь записей и упражнений** — по строке `WorkoutEntry.exerciseName`, **нет `exerciseId` и FK**. Переименование частично пробрасывается через `WorkoutDao.renameExercise()`. Удаление упражнения оставляет «висячие» записи.
- **Обработка ошибок БД** — реализована: `WorkoutViewModel.safeDb` обёртывает все мутации, ошибки публикуются в `errorMessage`, `MainActivity` показывает их в Snackbar.
- **Auto Backup** — флаг `android:allowBackup="true"` в манифесте есть, но `res/xml/backup_rules.xml` и `res/xml/data_extraction_rules.xml` **пустые шаблоны** без правил `<include>` для БД и DataStore. Бэкап работает с дефолтным поведением, но не гарантирует включение нужных файлов.
- **FAB «Добавить»** на `JournalScreen` — уже есть (`floatingActionButton = { FloatingActionButton(...) }`).
- **Таймер отдыха** в `ActiveWorkoutScreen` — реализован (`isResting`, `restTimeLeft`, composable `RestTimer`).
- **Числовые клавиатуры** в `AddEntryDialog` — `KeyboardType.Decimal` для веса, `KeyboardType.Number` для повторов.
- **`StatsScreen`** строит выпадающий список упражнений из справочника `allExercises` — смешения источников нет.
- **Тема** — Material 3 со своими токенами (`Volt`, `Obsidian`, `Carbon` и т.д.), но `GymProgressTheme(darkTheme = true)` **жёстко зашита** на тёмную тему. Светлая схема описана в `Theme.kt`, но не используется. Dynamic color (Material You) не подключён.
- **Навигация** — `NavigationSuiteScaffold` для табов (Журнал/Прогресс/Упражнения) + кастомный пункт «Ещё» с `DropdownMenu`. Вторичные экраны (история, настройки, тренер, активная тренировка, график) реализованы как overlay-флаги в `MainActivity` через `AppOverlayState` + `BackHandler`. Анимаций перехода между overlay нет.
- **Мёртвый код:** `WorkoutViewModel.exerciseNames` и `WorkoutDao.getAllExerciseNames()` объявлены, но не используются ни в одном экране.

---

## Принципы планирования

- **Не ломать** текущую single-ViewModel архитектуру и навигацию через `AppDestinations` + overlay-флаги. Все изменения — аддитивные.
- **Сортировка списков записей** во всех новых местах: `date ASC, id ASC`.
- **Никаких хардкодов цветов и отступов.** Только токены `MaterialTheme.colorScheme`, `GymTheme.colors`, `Spacing`, `Dimens`, `CardShape`, `FabShape`.
- **Оффлайн-first.** Сеть — опционально (только AI-подсказки и опциональная облачная синхронизация в будущем).
- **Все мутации БД и DataStore** — через `safeDb`/аналогичную обёртку, ошибки видны в Snackbar.

---

## Дорожная карта

| Фаза | Цель | Размер | Риск |
|------|------|--------|------|
| 1 | Резервное копирование (экспорт/импорт + правила Auto Backup) | M | средний |
| 2 | Быстрые UX-улучшения (PR-бейджи, степперы, swipe-undo, empty states) | M | низкий |
| 3 | Графики прогресса (диапазон дат, метрики, tooltip) | M | низкий |
| 4 | Дизайн-полировка (выбор темы, dynamic color, иконки табов) | S | низкий |
| 5 | Навигация (BottomSheet вместо DropdownMenu, анимации) | S | низкий |
| 6 | Активная тренировка: foreground service, звук/вибрация таймера | M | средний |
| 7 | Доступность и адаптивность (a11y, fontScale, two-pane на планшете) | M | низкий |
| 8 | Чистка техдолга (мёртвый код, FK по `exerciseId`, soft-delete) | M | средний |
| 9 | Опционально: облачная синхронизация через Google Drive `drive.file` | L | высокий |

Фазы 1, 2, 3, 4 максимально независимы и могут идти параллельно. Фаза 8 — самая инвазивная (миграция БД), её делать после стабилизации остальных.

---

## Фаза 1. Резервное копирование

Цель — гарантировать сохранность тренировочной истории:
- защита «по умолчанию» (Auto Backup),
- ручной перенос между устройствами (JSON),
- возможность анализа в Excel (CSV).

### 1.1 Дописать правила Auto Backup ✅ выполнено (2026-04-25)

`backup_rules.xml` и `data_extraction_rules.xml` заполнены явными `<include>`:
- БД Room (`gym_progress_db` + `-shm` / `-wal`),
- DataStore Preferences (`files/datastore/`).

`data_extraction_rules.xml` содержит оба блока — `<cloud-backup>` и `<device-transfer>`.

**Осталось:** проверить на тестовом устройстве сценарий «установить → добавить записи → переустановить с включённым Google Backup → данные восстановились».

### 1.2 Ручной экспорт/импорт JSON через SAF

Формат — JSON, человекочитаемо, единый файл со всем состоянием.

**Структура (`schemaVersion: 1`):**
```json
{
  "schemaVersion": 1,
  "appVersion": "1.x.x",
  "exportedAt": "2026-04-25T20:50:00Z",
  "settings": {
    "trainingGoal": "HYPERTROPHY",
    "scoringSystem": "SIMPLIFIED",
    "bodyWeightKg": 80.0,
    "heightCm": 180,
    "gender": "MALE"
  },
  "trainerSettings": { "...": "..." },
  "exercises": [
    {
      "clientId": "uuid-v4",
      "name": "Жим лёжа",
      "muscleGroup": "CHEST",
      "exerciseType": "COMPOUND",
      "isBodyweight": false
    }
  ],
  "entries": [
    {
      "clientId": "uuid-v4",
      "exerciseClientId": "uuid-v4",
      "date": "2026-04-25",
      "weight": 80.0,
      "reps": "10,8,6"
    }
  ]
}
```

**Ключевые решения, обоснованные кодом:**
- `date` — строка `yyyy-MM-dd` **как в БД**. Никакой конвертации в `Long`.
- `weight: Double`, `reps: String "n,n,n"` — формат `WorkoutEntry` 1:1.
- `clientId` (UUID v4) — **новое поле**, нужно добавить в обе таблицы:
  - `Exercise.clientId TEXT UNIQUE`,
  - `WorkoutEntry.clientId TEXT UNIQUE`.
  - Миграция (новая `MIGRATION_6_7`) генерирует UUID для существующих строк через `randomblob`/триггер или однократный `UPDATE` с UUID из приложения при первом старте после обновления.
- `exerciseClientId` — ссылка между `entries` и `exercises` без зависимости от `id`, который не стабилен между устройствами.
- `orderInDay` **не добавляем**: текущий `id` SQLite автоинкрементный, в пределах одной даты определяет порядок ввода. Сортировка `date ASC, id ASC` сохраняется. При импорте порядок воссоздаётся самим порядком вставки.

**Реализация (новые файлы):**
- `data/backup/BackupSchemaV1.kt` — DTO с `kotlinx.serialization`.
- `data/backup/BackupRepository.kt` — `exportJson(uri)`, `previewImport(uri)`, `applyImport(uri, strategy)`.
- `data/backup/ImportMerger.kt` — чистая функция merge без сайд-эффектов, тестируемая.
- Кнопки в `SettingsScreen.kt` или новый раздел «Резервное копирование».
- `ActivityResultContracts.CreateDocument("application/json")` / `OpenDocument(arrayOf("application/json"))`.

### 1.3 Стратегии импорта (две стратегии + отмена)

Чтобы не потерять реальные данные при ошибочном импорте.

#### Стратегия A. «Объединить (безопасно)» — по умолчанию

Пайплайн (в одной Room-транзакции `withTransaction`):
1. Прочитать файл, провалидировать `schemaVersion` (если новее — попросить обновить приложение, если старее — мигрировать формат).
2. Построить индексы текущей БД: `exerciseByClientId`, `exerciseByNormalizedName` (через `FormatUtils.normalizeExerciseNameKey`), `entryByClientId`, `entryByNaturalKey`.
3. **Упражнения:**
   - Совпадение по `clientId` → обновить поля.
   - Иначе совпадение по `normalizeExerciseNameKey(name)` → присвоить `clientId` из файла, обновить поля.
   - Иначе вставить новое.
4. **Записи:**
   - Совпадение по `clientId` → пропустить (история не должна перезаписываться).
   - Иначе совпадение по натуральному ключу `(exerciseClientId, date, weight, reps)` → присвоить `clientId` из файла, не вставлять дубликат.
   - Иначе вставить.
5. **Настройки и `trainerSettings`** — отдельная галочка «Также применить настройки из файла». По умолчанию **выключена**: настройки локального устройства считаются текущими.

**Гарантии:**
- Кол-во записей и упражнений **не уменьшается**.
- При любом исключении транзакция откатывается, БД нетронута.

#### Стратегия B. «Заменить всё»

Для сценария «новое устройство, нужна точная копия».
- Перед выполнением — **автобэкап текущей БД в файл** в кэш приложения (`backup_before_import_<timestamp>.json`, доступен 7 дней). Сообщение пользователю «Создан страховочный бэкап» с возможностью открыть/поделиться.
- Очистка таблиц + полная вставка из файла.
- **Двойное подтверждение** в диалоге: «Вы удалите N записей и M упражнений. Продолжить?» + поле подтверждения галочкой.

#### Стратегия C. «Отмена»

Закрыть диалог.

#### Экран предпросмотра

Перед применением — диалог:
```
Из файла: 124 записи, 18 упражнений (экспорт от 20.04.2026).
Сейчас в приложении: 98 записей, 15 упражнений.

Объединить:    +26 записей, +3 упражнения, 95 пропустим как дубликаты.
Заменить всё:  124 записи, 18 упражнений; текущие 98+15 удалятся.
```
Подсчёт — тот же merge-пайплайн в **dry-run** режиме (без записи).

### 1.4 Экспорт CSV журнала (без импорта)

Один CSV-файл — только для удобства анализа в Excel/Google Sheets.

- Кодировка UTF-8 c BOM (для корректного открытия в Excel под Windows).
- Разделитель `;`, десятичный разделитель — точка (Excel в RU-локали поймёт через мастер импорта, точка надёжнее в международном экспорте).
- Колонки: `Дата;Упражнение;Мышечная группа;Вес;Повторы;Объём (вес*сумма повторов)`.
- Дата в ISO `yyyy-MM-dd` (как в БД).
- 1RM/Score **не включаем** — это вычисляемые метрики, добавим, если попросят.
- Импорта CSV нет: формат не сохраняет настройки и `clientId`.

### 1.5 Тесты (минимум перед релизом фазы 1)

- Круговой: export → wipe → import-replace → побайтовое сравнение DTO.
- Merge: импорт в непустую БД, проверить отсутствие дубликатов и отсутствие потерь.
- Миграция формата файла v1 → актуальная (заглушка-каркас).
- Повреждённый JSON → понятная ошибка, БД нетронута.
- Транзакционность: искусственная ошибка в середине импорта → rollback.
- Производительность: 10 000 записей импортируются < 3 сек.

### 1.6 UI

В `SettingsScreen.kt` — новая секция «Резервное копирование»:
- «Экспорт JSON» (`CreateDocument("application/json")`, имя по умолчанию `gymprogress_<yyyyMMdd>.json`).
- «Импорт JSON» (`OpenDocument(arrayOf("application/json"))`) → `ImportPreviewDialog` → выбор стратегии.
- «Экспорт журнала в CSV» (`CreateDocument("text/csv")`).
- Краткая подсказка: «Auto Backup Google уже работает в фоне. Ручной экспорт нужен для переноса между устройствами или внешнего хранения».

---

## Фаза 2. Быстрые UX-улучшения

### 2.1 Подтверждение удаления через Snackbar Undo ✅ выполнено в Журнале (2026-04-25)
- В `JournalScreen` добавлен `pendingDelete: SnapshotStateMap<Long, WorkoutEntry>`.
- Удаление записи из диалога long-press → запись скрывается из UI (через `visibleTodayEntries`/`visiblePreviousExercises`), показывается `SnackbarHost` с действием «Отменить» (`SnackbarDuration.Short`).
- Если пользователь нажал «Отменить» — запись возвращается в UI, реального `onDeleteEntry()` не происходит.
- Если Snackbar истёк/был сброшен — вызывается `onDeleteEntry(entry)`.

**2026-04-25 (расширение):** тот же паттерн перенесён в `WorkoutHistoryScreen` — `pendingDelete` + `SnackbarHost` в `Scaffold`, удаление через `requestDeleteWithUndo`. `filteredEntries` строится из `visibleEntries` (entries без pending).

**Ограничения:**
- Локальный state экрана — при уходе и быстром возврате pending теряется (corutine отменилась → запись остаётся в БД). Это компромисс ради простоты; для жёстких гарантий нужно поднимать pending в ViewModel.

### 2.2 Степперы ± в `AddEntryDialog` ✅ выполнено (2026-04-25)
- Под полем веса — ряд `AssistChip` через `StepperRow`: −2.5 / −0.5 / +0.5 / +2.5.
- Рядом с каждым полем подхода — две `IconButton` (`KeyboardArrowDown` / `KeyboardArrowUp`) для −1/+1 повтора. Тач-таргет 48dp (стандартный размер `IconButton`).
- Парсинг и обновление через helper-функции `stepWeight(current, delta)` и `stepReps(current, delta)`. Пустое поле трактуется как 0; результат не уходит ниже нуля.
- Каждое нажатие даёт `haptics.tap()`.
- Степпер веса отключается, когда `weightFieldEnabled = false` (BW-упражнение без доп. веса).

### 2.3 PR-бейджи (Personal Records) ✅ выполнено (2026-04-25)
- В `MainActivity` вычисляется `personalRecordEntryIds: Set<Long>` — id записей с максимальным оценочным 1RM в рамках упражнения (через `SimplifiedScoreCalculator.calcE1RMForEntry`, группировка по нормализованному имени).
- `JournalScreen` (`WorkoutEntryCard`, `PreviousSessionExerciseRow`) и `WorkoutHistoryScreen` (`WorkoutEntryCard`) рисуют иконку `Icons.Default.EmojiEvents` цвета `Volt` рядом с именем упражнения, если запись — PR.
- Используется одна метрика (1RM) — самая интуитивная единица. PR по объёму и максимальному весу можно добавить позже как отдельные индикаторы.

**Не реализовано (для будущей итерации):**
- Snackbar «Новый рекорд!» при создании нового PR — нужно отслеживать факт прохождения через PR-границу при добавлении записи.
- Анимированная подсветка карточки.

### 2.4 Универсальный Empty State ✅ выполнено (2026-04-25)
- Создан `ui/components/EmptyState.kt` — параметры `icon`, `title`, `description`, опциональные `actionLabel` + `onAction`.
- Применён в `WorkoutHistoryScreen` (иконка `Inbox`, разные тексты для пустой даты и пустой истории) и `ExercisesScreen` (иконка `FitnessCenter`, CTA «Добавить упражнение» открывает диалог).
- Внутри: иконка `iconXl` (48dp) на скруглённом фоне `surfaceVariant` (`CardShape`, 80dp), `titleMedium` Bold, `bodyMedium` Secondary, опциональный `OutlinedButton` (`ButtonShape`).
- Эмпти-стейты внутри `JournalScreen` оставлены кастомными — они контекстные («Сегодня ещё нет тренировок» с разными подсказками в зависимости от наличия prev session).

### 2.5 «Повторить запись» ✅ выполнено (2026-04-25)
- В диалоге long-press по карточке (`JournalScreen`) добавлена кнопка «Повторить сегодня» — первая в списке (выше «Редактировать» и «Удалить»).
- Прокинут `onRepeatEntry: (WorkoutEntry) -> Unit` из `MainActivity` → `viewModel.addEntry(today, exerciseName, weight, reps)`. Создаёт новую запись на сегодняшнюю дату с тем же упражнением, весом и повторами.
- Swipe-жесты пока не реализованы — long-press диалог покрывает основной use case без введения новых жестов.

### 2.6 Превью «как изменится рекорд» в `AddEntryDialog` ✅ выполнено (2026-04-25)
- Под блоком подходов отображается строка: `1RM ≈ X.XX кг · +Δ к рекорду` (или `· Δ от рекорда` / `· ≈ рекорд`).
- Расчёт live через `SimplifiedScoreCalculator.calcE1RMForEntry(...)` на временной `WorkoutEntry`.
- Сравнивается с `bestEntry` (тем же, что используется в секции «Лучшая тренировка»).
- При превышении рекорда строка подсвечивается цветом `Volt` — мотивирующий маркер.
- Показывается только когда вес и хотя бы один валидный повтор введены; иначе — скрыто.
- Используется упрощённая система независимо от выбранной пользователем — 1RM в кг это интуитивная единица.

---

## Фаза 3. Графики прогресса

В `ExerciseProgressChartScreen.kt` была одна статичная кривая с автоосями. После итерации добавлены диапазон дат, переключатель метрики, обогащённый tooltip, PR-точки и линия тренда.

### 3.1 Диапазон дат ✅ выполнено (2026-04-26)
- Введён enum `data/ChartRange.kt`: `ONE_MONTH(30)`, `THREE_MONTHS(90)`, `SIX_MONTHS(180)`, `ONE_YEAR(365)`, `ALL(null)`.
- Хранение в DataStore (`SettingsRepository.chartRange` / `setChartRange`), `WorkoutViewModel.chartRange` StateFlow + `setChartRange`.
- Chip-row `ChartRangeChips` (FilterChip) над графиком, по умолчанию `THREE_MONTHS`.
- Фильтр применяется внутри `buildExerciseProgressChartPoints` после вычисления PR-флага: точки с датой раньше `today − range.days` отбрасываются, при `ALL` фильтр отсутствует.

### 3.2 Переключатель метрики ✅ выполнено (2026-04-26)
- Введён enum `data/ChartMetric.kt`: `E1RM` («1RM», кг), `VOLUME` («Объём», кг), `WORKING_WEIGHT` («Раб. вес», кг), `SCORE` («Score», без единицы).
- Хранение в DataStore (`SettingsRepository.chartMetric` / `setChartMetric`), `WorkoutViewModel.chartMetric` StateFlow + `setChartMetric`.
- Chip-row `ChartMetricChips` под чипами диапазона.
- `buildExerciseProgressChartPoints` принимает `metric` и для каждого дня выбирает «представителя» с максимумом по выбранной метрике; `ExerciseProgressChartPoint` теперь хранит сразу `e1rm`, `volume`, `workingWeight`, `score` представителя — для tooltip.
- Подпись оси Y и шаг сетки адаптивные: `niceStep` округляет шаг в ряд 1/2/5/10/20/50/100/200/500…, а не фиксирует 10. Подписи через `formatAxisLabel` (целое для VOLUME/SCORE, дробное для мелких 1RM).

### 3.3 Tooltip ✅ выполнено (2026-04-26)
- При тапе по точке открывается `AlertDialog` с датой (плюс маркер «· Рекорд» в цвете `success`, если PR), строкой подхода `вес × повторы` (как введено пользователем), объёмом, оценочным 1RM и баллом Score.
- Hit-радиус 40dp, ближайшая точка выбирается по `hypot`, как и раньше.

### 3.4 Линия тренда ✅ выполнено (2026-04-26)
- `computeTrend` — линейная регрессия по `yValue` от индекса точки; `null`, если точек < 2, нет дисперсии или диапазон дат меньше суток.
- Рисуется пунктиром (`PathEffect.dashPathEffect(8/6 dp)`) поверх основной линии цветом `onSurfaceVariant.copy(alpha = 0.7f)`.
- Под графиком — подпись `formatTrendText`: «Тренд: +0.8 кг/нед» / «−12 кг/нед» / «+25/нед» (для безразмерного Score). Знак, единица и формат числа подбираются автоматически.

### 3.5 PR-точки ✅ выполнено (2026-04-26)
- `ExerciseProgressChartPoint.isPersonalRecord` проставляется по полной истории до фильтра по диапазону: точка считается рекордом, если её `yValue` не меньше всех предыдущих (running max).
- На графике PR-точки рисуются увеличенным радиусом (9dp вместо 6dp) и обводкой `GymTheme.colors.success`. Внутренний кружок — `surface`, как у обычных точек.
- В Tooltip заголовок дополнительно подсвечивается зелёным, к дате добавляется «· Рекорд».

### 3.6 Прочее
- Цвет основной линии и обычных точек переключён с прямого `Volt` на `MaterialTheme.colorScheme.primary` (в текущей тёмной теме это тот же `Volt`, но через токен — на случай возвращения к светлой теме в фазе 4).
- `ExerciseProgressChartScreen` стал получать `chartRange`/`chartMetric` и обработчики извне (от `MainActivity` через `WorkoutViewModel`), `buildExerciseProgressChartPoints` больше не зависит от `ScoringSystem`: метрика выбирается явно.

---

## Фаза 4. Дизайн-полировка

### 4.1 Выбор темы пользователем ⛔ отменено (2026-04-26)
Изначально (2026-04-25) был добавлен выбор темы (`ThemeMode` SYSTEM/LIGHT/DARK), но светлая схема оказалась нерабочей: `GymTheme.colors.success` и другие токены были подобраны под тёмный бренд IRON CORE и не читаются на светлом фоне. Полный аудит контрастов (4.5) до релиза не помещался, поэтому **возможность выбора темы откатана**:

- `data/ThemeMode.kt` удалён.
- `GymProgressTheme` снова без параметров, использует только `DarkColorScheme` + `DarkExtendedColors` (как до 2026-04-25).
- В `SettingsRepository` убраны `themeMode` Flow и `setThemeMode` (ключ `theme_mode` в DataStore просто перестаёт читаться — при следующей записи он не воссоздаётся).
- В `WorkoutViewModel` убраны `themeMode` StateFlow и `setThemeMode`.
- В `MainActivity` убраны сбор `themeMode` и проброс в `GymProgressTheme`/`SettingsScreen`.
- В `SettingsScreen` удалён блок «Тема приложения» и параметры `currentThemeMode`/`onThemeModeChanged`.
- `LightColorScheme`/`LightExtendedColors` в `Theme.kt` остаются объявлёнными как заготовки под будущую светлую тему — их не использует никто.

Возвращаться к этому стоит только в связке с фазой 4.5 (полная ревизия контраста для светлой темы).

### 4.2 Dynamic color (Material You)

Опционально: на Android 12+ использовать `dynamicDarkColorScheme(context)`/`dynamicLightColorScheme(context)`. Управляется флагом «Использовать цвета системы» в настройках. Fallback — `IRON CORE` палитра.

**Риск:** dynamic color теряет «фирменный» Volt/Obsidian-стиль. Сделать **по запросу пользователя**, не по умолчанию.

### 4.3 Иконки табов ✅ выполнено (2026-04-25)
В `MainActivity.AppDestinations`:
- `JOURNAL` — оставлен `Icons.Default.DateRange`.
- `STATS` — `Icons.Default.Star` → `Icons.AutoMirrored.Filled.TrendingUp`.
- `EXERCISES` — `Icons.Default.List` (deprecated) → `Icons.Default.FitnessCenter`.

В рамках задачи добавлена зависимость `androidx.compose.material:material-icons-extended` (нужны были иконки за пределами core-набора). R8 в release-сборке уберёт неиспользуемые. Это разово открыло доступ ко всему extended-набору для будущих фаз (PR-бейджи, иконки таймера, счётчиков и т.п.).

### 4.4 Haptic feedback ✅ выполнено (2026-04-25)
- Создан хелпер `ui/components/HapticHelper.kt` с тремя уровнями: `tap()` — лёгкий отклик, `confirm()` — заметный для значимых событий, `warn()` — для деструктивных.
- `JournalScreen` — `tap()` на нажатие FAB.
- `ActiveWorkoutScreen` — `confirm()` при фиксации подхода.

Добавление отклика на PR-событие отложено до фазы 2.3.

### 4.5 Ревизия dark/light контраста
Прогнать все экраны в обеих темах после 4.1. Особое внимание: оси и сетка графика, `surfaceVariant` карточек, disabled states.

---

## Фаза 5. Навигация

### 5.1 «Ещё» как `ModalBottomSheet` ✅ выполнено (2026-04-25)
- `AppNavigationScaffold` изменён: вместо `DropdownMenu` в иконке «Ещё» используется `ModalBottomSheet` с `skipPartiallyExpanded = true`.
- Пункты меню — `MoreSheetItem` с иконкой и текстом, минимальный тач-таргет 56dp (`Spacing.huge`).
- Иконки: `Tune` (Настройки тренера), `Outlined.History` (История), `Settings`, `Info` (О приложении).
- При нажатии пункта — sheet плавно прячется, затем вызывается открытие оверлея.
- Пункт «Резервное копирование» будет добавлен вместе с фазой 1.2.

### 5.2 Анимации overlay-переходов ✅ выполнено (2026-04-25)
- `when (topOverlay)` в `MainActivity` обёрнут в `AnimatedContent` с `transitionSpec`, различающим push/pop по размеру стека:
  - **Push** (стек растёт) — новый экран въезжает справа, старый уходит влево на 1/4 экрана.
  - **Pop** (стек уменьшается) — старый уезжает вправо, подложка появляется слева.
  - **Open/close от root** — оверлей въезжает справа / уезжает вправо.
- Диалог «Новая запись» (`AddEntryDialog`) остался вне стека — у него своя анимация от Material `Dialog`.
- Длительность — 220 мс (`tween`).

### 5.3 Унификация back-логики ✅ выполнено (2026-04-25)
- `AppOverlayState` (data class с 9 boolean-флагами) и его Saver удалены.
- Добавлен sealed `AppOverlay` (`ProgressChart`, `WorkoutHistory`, `Settings`, `About`, `Trainer`, `TrainerSettings`, `ActiveWorkout`) в `ui/navigation/AppOverlay.kt`.
- Состояние — `SnapshotStateList<AppOverlay>` через `rememberSaveable(saver = OverlayStackSaver)` (сериализация в список строк).
- Единый `BackHandler(enabled = overlayStack.isNotEmpty())` в корне — обрабатывает system back по верху стека с особыми реакциями для `TrainerSettings` (сохранить настройки), `ActiveWorkout` (сбросить рекомендацию), `Trainer` (очистить AI-совет). Остальные — просто `pop`.
- Флаг `openedSettingsFromTrainer` удалён за ненужностью: стек сам разруливает возврат (Trainer → TrainerSettings → back возвращает к Trainer; «Ещё» → TrainerSettings → back в корень).
- Диалог `AddEntryDialog` остался отдельным boolean — он не fullscreen overlay.

Миграция на `NavHost` не делается — выгода маржинальна, риск высок.

---

## Фаза 6. Активная тренировка

Таймер уже есть. Что добавить:

### 6.1 Звук и вибрация на конец таймера
Сейчас `RestTimer` молча обнуляется. Добавить:
- 3 коротких бипа за 3/2/1 сек до конца + длинный по нулю (через `RingtoneManager`/`SoundPool`).
- Вибрацию (`Vibrator`/`VibratorManager`).
- В настройках — переключатели «Звук таймера», «Вибрация».

### 6.2 Foreground service во время сессии
Сейчас активная тренировка живёт только в Compose-стейте `ActiveWorkoutScreen`. Если пользователь свернул приложение и систем убил процесс — состояние теряется.

Завернуть активную тренировку в `ForegroundService` типа `health` (Android 14+) с уведомлением:
- Иконка + текущее упражнение + текущий подход + таймер.
- Действия в уведомлении: «Зафиксировать подход», «Завершить».
- Разрешение `POST_NOTIFICATIONS` на Android 13+ — запрашивать при первом старте тренировки.

### 6.3 Прогресс по тренировке
Линейный `LinearProgressIndicator` в шапке `ActiveWorkoutScreen`: выполнено / всего подходов.

### 6.4 Подсказка «как прошлый раз»
В блоке текущего подхода — чип «Прошлый раз: 80×8» (брать из `previousSession` или истории по упражнению). Тап заполняет поля.

---

## Фаза 7. Доступность и адаптивность

### 7.1 contentDescription
Прогнать все `Icon`/`IconButton` без текста — обязательный `contentDescription` (или `null` явно для декоративных). Запустить lint-правило `ContentDescription` как ошибку.

### 7.2 Тач-таргеты ≥ 48dp
Чек-лист по экранам, где могут быть мелкие иконочные кнопки: `JournalScreen` (карточки), графики, степперы.

### 7.3 fontScale
Прогнать с `Settings → Display → Font size = Largest` (≈ 1.3x). Не должно быть обрезок и наложений. Использовать `sp` для текста, `dp` для отступов.

### 7.4 TalkBack smoke-тест
Сценарии: добавить запись, открыть Прогресс, выбрать упражнение, экспорт JSON, активная тренировка.

### 7.5 Адаптивность (опционально)
`material3-window-size-class`. На `Expanded` (планшет, foldable развёрнут): двухпанельный режим Журнал слева + Прогресс справа. `NavigationSuiteScaffold` сам переключится на `NavigationRail`/`PermanentDrawer`, если включить нужный layoutType.

---

## Фаза 8. Чистка техдолга

### 8.1 Удалить мёртвый код ✅ выполнено (2026-04-25)
- Удалены `WorkoutViewModel.exerciseNames` и `WorkoutDao.getAllExerciseNames()`.
- Сборка `compileDebugKotlin` зелёная.

### 8.2 Case-insensitive уникальность упражнений + UI-проверка ✅ выполнено частично (2026-04-25)
- `ExerciseDao.countByNormalizedName(name, excludeId)` — SQL `LOWER(TRIM(REPLACE(name, char(160), ' ')))` для case-insensitive и неразрывных пробелов.
- `WorkoutViewModel.addExercise` и `updateExercise` проверяют `countByNormalizedName(normalizeExerciseNameKey(name))` до вставки. При дублях — понятное сообщение в `errorMessage` (Snackbar). При пустом имени — тоже ругаемся.

**Что осталось (отложено):**
- Миграция базы данных для индекса `COLLATE NOCASE` (или `nameKey`-колонки) — жёсткая защита на уровне СУБД. Сделаем вместе с фазой 8.3 (FK + clientId), чтобы все миграции схемы были в одном релизе.
- Маппинг `SQLiteConstraintException` в понятное сообщение (сейчас UI-проверки хватает в 99% сценариев, но возможны race conditions).

### 8.3 FK `WorkoutEntry.exerciseId → Exercise.id` (большой шаг)
Самая инвазивная правка. Делать в **одной связке** с фазой 1 (для бэкапа всё равно нужен `clientId`):
1. Миграция: добавить `Exercise.clientId TEXT UNIQUE` и `WorkoutEntry.clientId TEXT UNIQUE` и `WorkoutEntry.exerciseId INTEGER`.
2. Заполнить `exerciseId` через `JOIN ON exercises.name = workout_entries.exerciseName` (нормализуя пробелы/регистр через SQL `LOWER(TRIM(...))`).
3. Сирот (записи без совпадения по имени) — оставить с `exerciseId = NULL` или создать «архивные» упражнения автоматически.
4. Включить `ForeignKey(onDelete = SET_NULL)` или `CASCADE` (см. 8.4).
5. Заменить `WorkoutDao.getEntriesByExercise(name)` на запрос по `exerciseId`.
6. Поле `exerciseName` оставить как **денормализованное** для backwards compatibility и текстового поиска, обновлять при `renameExercise`.

**Риск:** миграция на пользовательских данных. Обязательно `androidTest` с реальным дампом.

### 8.4 Soft-delete упражнений (вместо CASCADE)
- `Exercise.isArchived: Boolean = false`.
- В списках выбора и каталоге архивные не показываются.
- История по архивному упражнению продолжает работать (есть `exerciseId`).
- В настройках раздел «Архив» с восстановлением.
- При клике «Удалить» в `ExercisesScreen` — спросить «Архивировать или удалить навсегда?».

---

## Фаза 9. Опционально — облачная синхронизация

Только если придёт пользовательский запрос. Рекомендованный путь — **личный Google Drive пользователя**:
- Авторизация через **Credential Manager API** (Google Sign-In deprecated).
- Scope `drive.file` — доступ только к файлам, созданным приложением; обычные файлы Drive недоступны.
- Приложение хранит `gymprogress_backup.json` в Drive App Folder, обновляет при изменениях.
- Конфликты: last-write-wins по `exportedAt` + предупреждение «Файл в Drive новее, чем локальные данные. Перезаписать локальные?».
- Управление: кнопка «Синхронизировать сейчас» + опционально расписание (раз в сутки, только Wi-Fi).

**Чего не делать:**
- Свой сервер с UUID-идентификацией — затраты на хостинг + GDPR/health data + риск потери данных при сбросе устройства.
- Firebase Anonymous Auth — vendor lock-in + те же проблемы потери данных.

---

## Сквозные требования (для всех фаз)

- Комментарии и сообщения пользователю — **русский**, имена символов и коммиты — **английский**.
- Только токены `MaterialTheme.colorScheme`/`GymTheme.colors`/`Spacing`/`Dimens`/`CardShape`/`FabShape`. Никаких `Color(0xFF...)` в новых местах.
- Сортировка списков записей — `date ASC, id ASC`.
- Все мутации БД и DataStore — через `safeDb` (или эквивалент), ошибки в Snackbar.
- Миграции Room — explicit, тест в `androidTest` с реальным дампом старой БД.
- Зависимости — только через `gradle/libs.versions.toml`.
- Версионирование приложения — автоматическое; `version.properties` не править вручную.

---

## Критерии готовности фазы

- [ ] Реализация соответствует принципам и сквозным требованиям.
- [ ] Покрыта тестами: unit для чистой логики, `androidTest` для БД и миграций.
- [ ] Ручная проверка на устройстве в обеих темах (после фазы 4 — авто/светлая/тёмная) и при `fontScale = 1.3`.
- [ ] Обновлён CHANGELOG (или release notes).
- [ ] Этот файл обновлён: фаза отмечена как выполненная.

---

## Открытые вопросы для согласования

1. **Стратегии импорта в фазе 1.3**: реализовать сразу обе («Объединить» + «Заменить всё») или начать только с «Объединить» + «Отмена»?
2. **Фаза 8.3 (FK по `exerciseId`)**: делать в связке с фазой 1 (один большой релиз с миграцией) или отложить до отдельного релиза?
3. **Фаза 4.2 (dynamic color)**: нужна ли вообще, учитывая брендовую палитру IRON CORE? Возможно, ограничиться выбором тёмная/светлая/системная (4.1).
4. **Фаза 6.2 (foreground service)**: делать сразу или после первой жалобы пользователя на потерю активной тренировки?
