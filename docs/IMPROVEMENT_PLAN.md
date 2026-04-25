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

### 1.1 Дописать правила Auto Backup

Текущие `backup_rules.xml` и `data_extraction_rules.xml` — заглушки. Дописать `<include>` блоки:

- БД Room: `domain="database"`, путь `gym_progress_db` (имя из `AppDatabase.getDatabase`).
- DataStore-настройки: `domain="file"`, путь `datastore/settings.preferences_pb` (имя `settings` из `SettingsRepository`).
- Исключить: кэши, `BuildConfig`-секреты в бэкап и так не попадают.

В `data_extraction_rules.xml` заполнить и `<cloud-backup>`, и `<device-transfer>` — Android 12+ использует разные блоки для бэкапа в Google и для прямой передачи устройство-устройство.

**Готово, когда:** на тестовом устройстве после переустановки приложения с включённым «Backup by Google One» восстанавливаются записи и настройки.

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

### 2.1 Подтверждение удаления через Snackbar Undo

Сейчас `onDeleteEntry`/`onDeleteExercise` исполняются мгновенно, без отмены. Заменить на soft-delete:
- Удалить из UI-состояния сразу.
- Snackbar «Запись удалена. Отменить» (5 сек, `LaunchedEffect`).
- По истечении — реальный `workoutDao.delete()`.
- По нажатию Undo — вернуть в UI без обращения к БД.

Это базовый Material pattern, минимизирует риск случайной потери записи.

### 2.2 Степперы ± в `AddEntryDialog`

Сейчас вес и повторы вводятся только клавиатурой. Добавить ряд кнопок:
- Для веса: `-2.5 / -0.5 / +0.5 / +2.5` под полем.
- Для повторов: `-1 / +1` рядом с каждым подходом.
- Каждая кнопка ≥ 48dp по тач-таргету.
- Лёгкий haptic (`LocalHapticFeedback.HapticFeedbackType.LongPress`/`TextHandleMove`).

Особенно полезно во время тренировки между подходами.

### 2.3 PR-бейджи (Personal Records)

Считать рекорды по упражнению по трём осям:
- максимальный вес (за 1 повторение в подходе),
- максимальный подходовый объём (`weight × max(reps)`),
- максимальный 1RM (через `SimplifiedScoreCalculator.estimate1Rm` или эквивалент).

В `JournalScreen` и `WorkoutHistoryScreen` на карточке записи — иконка-бейдж (`Icons.Default.EmojiEvents` или `Icons.Default.Star` цвета `GymTheme.colors.success`) при попадании записи в любую из трёх категорий. Tooltip/субтитр: «Рекорд: 1RM».

При создании новой записи-PR — кратковременная Snackbar «Новый рекорд!». Подсветка карточки `GymTheme.colors.successContainer` на 2 секунды.

### 2.4 Универсальный Empty State

Сейчас в каждом экране пустое состояние решается по-своему. Создать `ui/components/EmptyState.kt`:
```kotlin
@Composable
fun EmptyState(icon: ImageVector, title: String, description: String, primaryAction: (() -> Unit)? = null, actionLabel: String? = null)
```

Использовать в `JournalScreen` (пустой день), `StatsScreen` (нет упражнений / нет записей), `ExercisesScreen`, `WorkoutHistoryScreen`.

### 2.5 «Повторить запись»

В `JournalScreen` swipe вправо по карточке предыдущей записи или в меню долгого тапа — «Повторить сегодня». Создаёт `WorkoutEntry` с `date = today`, тем же упражнением, весом и повторами. Решает основной use-case прогрессии: «вчера 60×8, сегодня делаю то же».

### 2.6 Превью «как изменится рекорд» в `AddEntryDialog`

Под полем веса/повторов — небольшой текст «1RM: 95 кг (+1.5 к рекорду)». Использовать существующий `scoringEngine` из ViewModel. Расчёт live при изменении полей.

---

## Фаза 3. Графики прогресса

В `ExerciseProgressChartScreen.kt` сейчас один статичный график с автоосями.

### 3.1 Диапазон дат
Chip-row над графиком: **1М / 3М / 6М / 1Г / Всё**. По умолчанию 3М. Сохранять выбор в DataStore (`chartRangePref`).

### 3.2 Переключатель метрики
Chip-row: **Объём / 1RM / Рабочий вес / Score**. Переключатель влияет на `yValue` точек.

### 3.3 Tooltip
Тап/drag по графику → пузырёк с датой, весом×повторами, 1RM, Score. Рисуется поверх Canvas через `pointerInput`.

### 3.4 Линия тренда (опционально)
Линейная регрессия по выбранной метрике, рисуется пунктиром поверх. Подпись «Тренд: +0.8 кг/нед».

### 3.5 PR-точки
Точки-рекорды — увеличенный радиус и цвет `GymTheme.colors.success`.

---

## Фаза 4. Дизайн-полировка

### 4.1 Выбор темы пользователем
Сейчас в `Theme.kt` `darkTheme: Boolean = true` зашит. Добавить:
- В `SettingsRepository` — `themeMode: Flow<ThemeMode>` (`SYSTEM / LIGHT / DARK`), по умолчанию `SYSTEM`.
- В `Theme.kt` — `darkTheme` вычислять как:
  ```
  when (themeMode) { SYSTEM -> isSystemInDarkTheme(); LIGHT -> false; DARK -> true }
  ```
- В `SettingsScreen` — переключатель «Тема приложения: Авто/Светлая/Тёмная».

Светлая схема в `Theme.kt` уже описана — нужно лишь дать пользователю возможность её включить.

### 4.2 Dynamic color (Material You)

Опционально: на Android 12+ использовать `dynamicDarkColorScheme(context)`/`dynamicLightColorScheme(context)`. Управляется флагом «Использовать цвета системы» в настройках. Fallback — `IRON CORE` палитра.

**Риск:** dynamic color теряет «фирменный» Volt/Obsidian-стиль. Сделать **по запросу пользователя**, не по умолчанию.

### 4.3 Иконки табов
В `MainActivity.AppDestinations`:
- `JOURNAL` (`Icons.Default.DateRange`) → можно оставить или `Icons.Default.Today`.
- `STATS` (`Icons.Default.Star`) → `Icons.AutoMirrored.Filled.TrendingUp` (звезда плохо передаёт «прогресс»).
- `EXERCISES` (`Icons.Default.List`) → `Icons.Default.FitnessCenter`.

### 4.4 Haptic feedback
Лёгкий `LocalHapticFeedback` на: фиксацию подхода в активной тренировке, нажатие FAB, новый PR. Сделать единый `HapticHelper` для централизации.

### 4.5 Ревизия dark/light контраста
Прогнать все экраны в обеих темах после 4.1. Особое внимание: оси и сетка графика, `surfaceVariant` карточек, disabled states.

---

## Фаза 5. Навигация

### 5.1 «Ещё» как `ModalBottomSheet`
Заменить `DropdownMenu` в `AppNavigationScaffold.kt:54-104` на `ModalBottomSheet`. Причины:
- `DropdownMenu` плохо якорится в `NavigationRail`/`PermanentDrawer` при адаптивных режимах `NavigationSuiteScaffold`.
- Тач-таргеты больше (56dp), нет проблем с обрезкой.

Содержимое: список `ListItem` — «Настройки тренера», «История тренировок», «Резервное копирование» (после фазы 1), «Настройки», «О приложении».

### 5.2 Анимации overlay-переходов
Обернуть оверлей-ветки в `MainActivity:167-305` в `AnimatedContent`/`AnimatedVisibility` со `slideInHorizontally`/`fadeIn`. Это аддитивная правка без переписывания структуры.

### 5.3 Унификация back-логики
Сейчас `BackHandler` в каждом overlay блоке дублирует логику закрытия и есть ручной `openedSettingsFromTrainer` флаг (`MainActivity:233-253`). Вынести в `OverlayStack`-helper:
```kotlin
class OverlayStack {
    fun push(overlay: OverlayId)
    fun pop(): OverlayId?
    @Composable fun BackHandler()
}
```
Уменьшит риск рассинхрона флагов.

**Не делать** полную миграцию на `NavHost` — это переломный рефакторинг, выгода маржинальна.

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

### 8.1 Удалить мёртвый код
- `WorkoutViewModel.exerciseNames` (строки 81-82) — не используется.
- `WorkoutDao.getAllExerciseNames()` (строки 30-31) — не используется.
- Удалить вместе с импортами.

### 8.2 Case-insensitive уникальность упражнений + UI-проверка
- Миграция `MIGRATION_x_y`: пересоздать индекс с `COLLATE NOCASE` (или нормализовать `name` в новой колонке `nameKey` и индекс на ней).
- В `ExercisesScreen` перед вставкой/обновлением вызывать `exerciseDao.countByName(normalized)` и показывать понятный текст «Упражнение с таким именем уже есть».
- Сейчас отлавливаем `SQLiteConstraintException` в `safeDb`, но сообщение «UNIQUE constraint failed» — не для пользователя.

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
