# Design

## Context

См. `proposal.md` — Why. Текущее состояние, важное для реализации:

- Room версии 7: у `Exercise` и `WorkoutEntry` уже есть стабильные `clientId`; у записи есть `exerciseId` с FK и денормализованный `exerciseName`. Отдельная миграция БД для бэкапа не нужна.
- Все пользовательские настройки лежат в одном DataStore Preferences (`SettingsRepository`), включая `trainerSettings`, антропометрию, тему графика и дату абонемента.
- `SettingsScreen` — единственный экран настроек; глобальные ошибки показываются через `WorkoutViewModel.errorMessage` и Snackbar в `MainActivity`.
- SAF-диалоги (`CreateDocument`/`OpenDocument`) в проекте ещё не используются; `kotlinx.serialization` и `FileProvider` не подключены.
- Списки записей уже сортируются `date ASC, id ASC`; CSV должен сохранить этот порядок.

## Goals / Non-Goals

**Goals:**

- Один версионированный JSON-формат для полного состояния приложения.
- Безопасное объединение без потери данных и точная замена с страховочным бэкапом.
- Предпросмотр до любой записи в БД.
- CSV-выгрузка журнала для Excel/Sheets.
- Ошибки чтения/записи и повреждённого файла не должны менять БД.

**Non-Goals:**

- Импорт CSV.
- Облачная синхронизация и разрешение конфликтов между устройствами.
- Шифрование бэкапа и пароль на файл.
- Автоматический периодический экспорт.
- Изменение схемы Room или формата дат.

## Decisions

### 1. Формат JSON

`schemaVersion: 1`; DTO с `kotlinx.serialization`:

```json
{
  "schemaVersion": 1,
  "appVersion": "1.0.634",
  "exportedAt": "2026-10-31T12:00:00Z",
  "settings": {
    "trainingGoal": "HYPERTROPHY",
    "scoringSystem": "SIMPLIFIED",
    "bodyWeightKg": 80.0,
    "heightCm": 180,
    "gender": "MALE",
    "chartRange": "THREE_MONTHS",
    "chartMetric": "E1RM",
    "timerSoundEnabled": true,
    "timerVibrationEnabled": true,
    "membershipExpiryDate": "2026-12-31"
  },
  "trainerSettings": {
    "splitType": "FULL_BODY",
    "daysPerWeek": 3,
    "priorityGroups": ["CHEST"],
    "customSplitDays": [{"dayIndex": 0, "groups": ["CHEST", "BACK"]}],
    "includeWarmup": true,
    "autoDeload": true,
    "deloadIntervalWeeks": 4,
    "progressionType": "DOUBLE"
  },
  "exercises": [
    {
      "clientId": "uuid",
      "name": "Жим лёжа",
      "muscleGroup": "CHEST",
      "exerciseType": "COMPOUND",
      "isBodyweight": false,
      "isArchived": false
    }
  ],
  "entries": [
    {
      "clientId": "uuid",
      "exerciseClientId": "uuid",
      "exerciseName": "Жим лёжа",
      "date": "2026-10-31",
      "weight": 80.0,
      "reps": "10,8,6"
    }
  ]
}
```

Решения:

- Настройки и enum-значения хранятся строками, а не Kotlin-типами: неизвестное значение можно игнорировать, не роняя импорт.
- `exerciseName` есть у каждой записи: это нужно для «сиротских» записей после окончательного удаления упражнения.
- `exerciseClientId` — переносимая ссылка; локальный `exerciseId` в файл не попадает.
- `nameKey` не экспортируется: он вычисляется из имени тем же `FormatUtils.normalizeExerciseNameKey`.
- `Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }`.

### 2. Слои и ответственность

- `BackupSchemaV1.kt` — DTO `@Serializable`.
- `BackupCsv.kt` — чистая функция `buildCsv(exercises, entries): String` (BOM, экранирование, объём).
- `ImportMerger.kt` — чистая функция `planMerge(current, backup): MergePlan`; не пишет в БД, покрывается unit-тестами.
- `BackupRepository.kt` — чтение/запись SAF-файлов, preview, применение merge/replace в `db.withTransaction`, создание страховочного бэкапа.
- `SettingsRepository` — `readSnapshot()` и `applySnapshot()`; запись всех настроек одним `dataStore.edit`.

`WorkoutViewModel` создаёт `BackupRepository` и отдаёт UI состояние `BackupUiState` (preview, busy, message, safetyBackupUri) и методы экспорта/импорта.

### 3. Объединение

`planMerge` работает на снимке текущих упражнений и записей, без записи:

1. Индексы текущей базы: `exerciseByClientId`, `exerciseByNameKey`, `entryByClientId`, `entryByNaturalKey`.
2. Упражнения:
   - совпадение по `clientId` → обновить поля, сохранить локальный `id` и `isArchived`;
   - иначе по `nameKey` → обновить поля, сохранить локальный `clientId`; файловый `clientId` запомнить в карте соответствий;
   - иначе создать новое упражнение с `clientId` из файла.
3. Записи:
   - совпадение по `clientId` → пропустить (история не перезаписывается);
   - иначе по натуральному ключу `(exerciseId или нормализованное имя, date, weight, reps)` → пропустить;
   - иначе создать запись; `exerciseId` берётся из карты `exerciseClientId`, `exerciseName` — из локального упражнения или из файла для сироты.
4. `MergePlan` содержит списки новых/обновлённых сущностей и счётчики для preview.

Локальный `clientId` существующего упражнения не перезаписывается файловым: это сохраняет стабильность ссылок на устройстве. Файловый `clientId` используется только для сопоставления в текущем импорте.

### 4. Замена

- Сначала `createSafetyBackup()` сериализует текущее состояние в `context.cacheDir/backups/backup_before_import_<timestamp>.json`. Если файл не создан — импорт не начинается.
- Затем `db.withTransaction`: `deleteAllEntries()` → `deleteAllExercises()` → вставка упражнений → вставка записей.
- Упражнения создаются с `clientId` из файла и `nameKey` из имени; записи — с `exerciseId`, найденным по `exerciseClientId`, либо `null`.
- `id` не сохраняются: порядок в пределах дня восстанавливается порядком вставки (как в текущем импорте по плану).
- Двойное подтверждение: первый диалог с количествами, второй — с галочкой «Я понимаю, что текущие данные будут заменены».

### 5. Настройки

- `BackupSettingsV1` и `BackupTrainerSettingsV1` содержат nullable-поля. `applySnapshot` применяет только непустые и валидные значения; неизвестные enum-строки игнорируются, текущее значение остаётся.
- Галочка «Также применить настройки из файла» по умолчанию выключена.
- Настройки применяются после успешной транзакции данных; ошибка DataStore не откатывает уже импортированные данные, но показывается пользователю.

### 6. CSV

- `\uFEFF` + заголовок `Дата;Упражнение;Мышечная группа;Вес;Повторы;Объём`.
- Дата — ISO из БД; вес — `FormatUtils.formatWeight`; повторы — как в `WorkoutEntry.reps`; объём — `weight * sum(reps)`.
- Поля с `;`, кавычками или переводом строки экранируются: значение в кавычках, внутренние кавычки удваиваются.
- Строки — `date ASC, id ASC`; мышечная группа — русское `displayName`, иначе исходная строка.
- CSV пишется в UTF-8 без BOM внутри строки (BOM — первый символ файла).

### 7. UI и SAF

В `SettingsScreen` добавляется секция «Резервное копирование»:

- «Экспорт JSON» → `CreateDocument("application/json")`, имя `gymprogress_<yyyyMMdd>.json`.
- «Импорт JSON» → `OpenDocument(arrayOf("application/json"))` → preview-dialog.
- «Экспорт журнала в CSV» → `CreateDocument("text/csv")`, имя `gymprogress_journal_<yyyyMMdd>.csv`.
- Preview показывает: файл (N записей, M упражнений, дата экспорта), текущая база, ожидаемые добавления/совпадения; для замены — сколько удалится.
- Кнопки: «Объединить», «Заменить всё», «Отмена». Для замены — второй диалог с галочкой.
- Галочка «Также применить настройки из файла» — в preview.
- Сообщения об успехе/ошибке — локальный `SnackbarHost` в `SettingsScreen`; глобальный `errorMessage` остаётся для остальных ошибок.
- Страховочный бэкап шарится через `FileProvider` (`${applicationId}.fileprovider`, `cache-path backups/`), action «Поделиться» в Snackbar.

### 8. Тестирование

- Unit: `ImportMergerTest` (clientId, nameKey, натуральный ключ, архив, сирота, счётчики), `BackupCsvTest` (BOM, заголовок, экранирование, объём, порядок), `BackupSerializationTest` (round-trip, неизвестные поля, schemaVersion).
- AndroidTest: merge в in-memory Room, замена, откат при ошибке, сохранение архивных и сиротских записей; экспорт/импорт через файл `Uri` в кэше.
- Ручное: экспорт → очистка данных → импорт-замена; импорт-объединение в непустую базу; повреждённый JSON; отмена SAF; CSV в Excel/Sheets; 10 000 записей.
- Сборка: `testDebugUnitTest`, `connectedDebugAndroidTest`, `lintDebug`, `assembleDebug`.

## Risks / Trade-offs

- **Потеря данных при замене** → страховочный бэкап создаётся до транзакции; при ошибке бэкапа импорт не начинается; транзакция откатывается.
- **Дубликаты из-за разных `clientId`** → сопоставление по `nameKey` и натуральному ключу; локальный `clientId` сохраняется.
- **Конфликт `clientId` записей** → merge пропускает существующую запись; replace использует файл.
- **Повреждённый/огромный JSON** → валидация схемы, ограничение размера файла и понятная ошибка; тест на повреждённый файл.
- **Настройки из чужого устройства** → галочка выключена по умолчанию; невалидные значения игнорируются.
- **CSV и Excel** → BOM и `;` выбраны под Windows/Excel; спецсимволы экранируются.
- **Страховочный бэкап в кэше** → файл может быть удалён системой; шаринг через FileProvider; в интерфейсе указано, что это временная копия.

## Migration Plan

1. Добавить DTO, сериализацию и зависимости.
2. Реализовать CSV и `ImportMerger`, покрыть unit-тестами.
3. Реализовать `BackupRepository`, методы SettingsRepository, DAO `deleteAll`, preview/merge/replace.
4. Подключить ViewModel и UI, FileProvider.
5. Прогнать unit/androidTest/lint/assemble, ручные сценарии.
6. Обновить `docs/IMPROVEMENT_PLAN.md`, `CHANGELOG.md`; архивировать change после ревью.

## Open Questions

Нет.
