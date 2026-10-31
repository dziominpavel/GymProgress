# Tasks

## 1. Схема и тестовая инфраструктура

- [x] 1.1 Включить `exportSchema = true`, добавить KSP-аргумент `room.schemaLocation` и каталог схем в `androidTest` assets; собрать проект и проверить, что `app/schemas/<package>/6.json` создан.
- [x] 1.2 Добавить `androidx.room:room-testing` в `gradle/libs.versions.toml` и `androidTestImplementation`; проверить, что `./gradlew :app:compileDebugAndroidTestKotlin` проходит.
- [x] 1.3 Добавить в `Exercise` поля `clientId`, `nameKey`, `isArchived` и уникальные индексы, в `WorkoutEntry` — `clientId`, `exerciseId` с FK `ON DELETE SET NULL` и индексы; проверить, что `./gradlew :app:compileDebugKotlin` проходит.
- [x] 1.4 Добавить unit-тесты `FormatUtils.normalizeExerciseNameKey` для кириллицы, NBSP и повторяющихся пробелов; проверить `./gradlew :app:testDebugUnitTest`.

## 2. Миграция 6 → 7

- [x] 2.1 Вынести чистую функцию выбора канонического упражнения при слиянии дубликатов (по числу записей, затем по `id`) и покрыть её unit-тестом; проверить `testDebugUnitTest`.
- [x] 2.2 Реализовать `MIGRATION_6_7`: добавить колонки, заполнить `clientId` и `nameKey`, объединить дубликаты, проставить `exerciseId`, пересобрать таблицы с FK и индексами; подключить миграцию и поднять версию БД до 7, проверить `compileDebugKotlin`.
- [x] 2.3 Написать `MigrationTestHelper`-тест на схеме версии 6 с дубликатами, кириллицей в разном регистре, NBSP, «сиротской» записью и bodyweight-упражнением; проверить сохранение количества записей, уникальность `clientId`/`nameKey`, заполнение `exerciseId`, `NULL` у сироты и `SET NULL` при удалении.
- [x] 2.4 Обновить `docs/POTENTIAL_ERRORS_ANALYSIS.md` (пункты 1 и 2) и `docs/IMPROVEMENT_PLAN.md` (8.2–8.4), отметив фактический статус; проверить, что описание совпадает с реализованной схемой и UI.

## 3. DAO и ViewModel

- [x] 3.1 Добавить в `ExerciseDao` запросы активных упражнений и поиска по `nameKey`, в `WorkoutDao` — выборку по `exerciseId`, обновление имени связанных записей и подсчёт записей; проверить `compileDebugKotlin`.
- [x] 3.2 Добавить instrumented-тест DAO на архивирование, восстановление, удаление с `SET NULL` и выборку записей по `exerciseId`; проверить на устройстве или эмуляторе.
- [x] 3.3 Обновить `WorkoutViewModel.addExercise`/`updateExercise`: вычислять `nameKey`, проверять дубликаты и обновлять имена связанных записей в одной транзакции; проверить unit-тестами и ручным сценарием переименования.
- [x] 3.4 Добавить `archiveExercise`, `restoreExercise`, `deleteExercisePermanently`; проверить, что архивирование не меняет записи, восстановление возвращает упражнение, а окончательное удаление оставляет записи с `exerciseId = null`.
- [x] 3.5 Обновить `addEntry` и `saveCompletedWorkout`: резолвить `exerciseId`, блокировать архивное упражнение с сообщением, разрешать «сиротскую» запись для удалённого; проверить ручным сценарием повтора.
- [x] 3.6 Обновить `entriesForSelectedExercise`: фильтровать по `exerciseId`, с fallback на нормализованное имя для записей без связи; проверить, что статистика не теряет старые записи.

## 4. UI и точки выбора

- [x] 4.1 Добавить в `ExercisesScreen` режим «Архив», действия «В архив», «Восстановить», «Удалить навсегда» и предупреждение в диалоге; прокинуть колбэки из `MainActivity`, проверить вручную на устройстве.
- [x] 4.2 Отфильтровать архивные упражнения в `AddEntryDialog`, а в `EditEntryDialog` искать `isBodyweight` по `exerciseId`; проверить ручным сценарием новой записи и редактирования.
- [x] 4.3 Обновить `StatsScreen`, `JournalScreen` и `WorkoutHistoryScreen`: пометка «(архив)», отображение имени, PR-группировка по `exerciseId` с fallback, sparkline по `exerciseId`; проверить, что история и графики архивного упражнения доступны.
- [x] 4.4 Исключить архивные упражнения из кандидатов `TrainerRecommendationEngine` и из рекомендации для журнала, сохранив их для разбора истории; проверить, что тренер не предлагает архивное упражнение.
- [x] 4.5 Добавить пользовательские тексты и `contentDescription` для новых действий, проверить `./gradlew :app:lintDebug` без ошибок ContentDescription.

## 5. Проверка и релиз

- [x] 5.1 Прогнать `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`; проверить, что все задачи завершились успешно.
- [x] 5.2 Обновить реальный дамп БД версии 6 на устройстве и проверить: количество записей не изменилось, дубликаты объединены, архив/восстановление/переименование/окончательное удаление работают, сортировка `date ASC, id ASC` сохранена.
- [x] 5.3 Добавить запись в `CHANGELOG.md` в `## [Unreleased]`: пользовательский блок (`**Добавлено**`, `**Исправлено**`) и инженерные детали под `###`; проверить формат по `docs/versioning.md`.
- [x] 5.4 Выполнить `openspec validate --all --strict` и `openspec status --change exercise-identity-and-archive`; проверить, что change валиден и все артефакты отмечены done.

## Workflow follow-up

- Заархивировать change после ревью и проверки релиза.
- После архивации создать change `backup-export-import`, опирающийся на `clientId` и `exerciseId`.
