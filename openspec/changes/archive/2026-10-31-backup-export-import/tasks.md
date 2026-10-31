# Tasks

## 1. Зависимости и формат файла

- [x] 1.1 Добавить плагин `kotlinx.serialization` и зависимость `kotlinx-serialization-json` в `gradle/libs.versions.toml`/`app/build.gradle.kts`; проверить `./gradlew :app:compileDebugKotlin`.
- [x] 1.2 Создать DTO `BackupFileV1`, `BackupSettingsV1`, `BackupTrainerSettingsV1`, `BackupExerciseV1`, `BackupEntryV1` с `@Serializable` и JSON-конфигурацией (`ignoreUnknownKeys`, `encodeDefaults`, `prettyPrint`); проверить unit-тестом round-trip и неизвестных полей.
- [x] 1.3 Добавить `FileProvider` в `AndroidManifest.xml`, `res/xml/backup_file_paths.xml` с `cache-path backups/`; проверить `./gradlew :app:assembleDebug`.

## 2. CSV и слияние

- [x] 2.1 Реализовать `BackupCsv.buildCsv`: BOM, заголовок, экранирование `;`/кавычек/переводов строк, объём и порядок `date ASC, id ASC`; проверить unit-тестами.
- [x] 2.2 Реализовать чистый `ImportMerger.planMerge`: индексы по `clientId`, `nameKey`, `entryClientId` и натуральному ключу; проверить unit-тестами на дубликаты, архивные и «сиротские» записи.
- [x] 2.3 Проверить счётчики `MergePlan` для preview (добавлено/совпало/обновлено); проверить unit-тестами на непустой и пустой базе.

## 3. Настройки и DAO

- [x] 3.1 Добавить в `SettingsRepository` `readSnapshot()` и `applySnapshot()` с одной записью DataStore; неизвестные значения игнорировать; проверить unit/androidTest-тестом снимка и применения.
- [x] 3.2 Добавить в `ExerciseDao`/`WorkoutDao` `deleteAll`, `getAllOnce` и пакетную вставку упражнений; проверить instrumented-тестом в in-memory Room.
- [x] 3.3 Проверить, что `applySnapshot` не перезаписывает текущие настройки при `null`/неизвестных значениях; проверить тестом с неполным JSON.

## 4. Repository

- [x] 4.1 Реализовать `BackupRepository.exportJson(uri)`: снимок БД и настроек, сериализация, запись через `ContentResolver`; проверить androidTest с файлом в кэше.
- [x] 4.2 Реализовать `BackupRepository.readPreview(uri)`: валидация `schemaVersion`, обязательных полей и подсчёт merge/replace без записи; проверить тестами на валидный, повреждённый и слишком новый JSON.
- [x] 4.3 Реализовать `BackupRepository.importMerge(uri, applySettings)`: `db.withTransaction`, `planMerge`, вставка/обновление, карта `exerciseClientId`; проверить androidTest на непустой базе и откат при ошибке.
- [x] 4.4 Реализовать `BackupRepository.importReplace(uri, applySettings)`: страховочный JSON в `cacheDir/backups`, `deleteAll` и вставка в одной транзакции; проверить androidTest и сценарий «бэкап не создан — импорт не начат».
- [x] 4.5 Реализовать `exportCsv(uri)` и `createSafetyBackup()` с `FileProvider`-Uri; проверить unit/Android-тестами и `assembleDebug`.

## 5. ViewModel и UI

- [x] 5.1 Добавить в `WorkoutViewModel` `BackupUiState` и методы export/preview/import/csv/safety-backup; ошибки публиковать в `errorMessage`, успехи — в состоянии; проверить `compileDebugKotlin`.
- [x] 5.2 Добавить в `SettingsScreen` раздел «Резервное копирование» и SAF-лаунчеры `CreateDocument`/`OpenDocument` с именами файлов; проверить ручным сценарием выбора файла.
- [x] 5.3 Добавить `ImportPreviewDialog`: количества файла/базы, кнопки «Объединить», «Заменить всё», «Отмена», галочку применения настроек; проверить ручным сценарием.
- [x] 5.4 Добавить второе подтверждение для «Заменить всё» с галочкой и показ количества удаляемых данных; проверить, что отмена не меняет БД.
- [x] 5.5 Показать успех/ошибку через Snackbar, добавить action «Поделиться» для страховочного бэкапа; проверить `contentDescription` и `./gradlew :app:lintDebug`.
- [x] 5.6 Прокинуть состояние и колбэки из `MainActivity` в `SettingsScreen`; проверить `assembleDebug`.

## 6. Проверка и релиз

- [x] 6.1 Прогнать `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`; проверить успешное завершение.
- [x] 6.2 Прогнать `./gradlew :app:connectedDebugAndroidTest`; проверить merge/replace/rollback и файловые операции.
- [x] 6.3 Ручной сценарий: экспорт JSON → очистка данных → импорт-замена → совпадение количества и содержимого; проверить на устройстве.
- [x] 6.4 Ручной сценарий: импорт-объединение в непустую базу, повреждённый JSON, отмена SAF, CSV в Excel/Sheets, 10 000 записей; проверить отсутствие потерь и понятные ошибки.
- [x] 6.5 Обновить `docs/IMPROVEMENT_PLAN.md` (фаза 1) и `CHANGELOG.md` (`[Unreleased]`); проверить формат changelog по `docs/versioning.md`.
- [x] 6.6 Выполнить `openspec validate --all --strict` и `openspec status --change backup-export-import`; проверить, что change валиден и все артефакты готовы.

## Workflow follow-up

- Заархивировать change после ревью и проверки релиза.
