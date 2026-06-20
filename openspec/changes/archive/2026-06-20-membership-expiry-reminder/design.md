## Context

В приложении уже есть один NotificationChannel (`active_workout`, `IMPORTANCE_LOW`, silent) для foreground-сервиса активной тренировки. Этот канал не подходит для напоминаний о абонементе: он silent, ongoing и живёт только во время тренировки.

Существующая инфраструктура:
- `SettingsRepository` — DataStore Preferences, паттерн `Flow<T> + suspend setX(...)` с `stringPreferencesKey` / `intPreferencesKey` / `booleanPreferencesKey`.
- `AddEntryDialog.kt` (строки 709–739) — готовый паттерн `DatePickerDialog` + `rememberDatePickerState` + `DatePicker` с конвертацией `selectedDateMillis → LocalDate → FormatUtils.toStorageDate`.
- `WorkoutViewModel` — единственный ViewModel, через который экраны получают данные и колбэки.
- `SettingsScreen.kt` — экран настроек с блоками «Система оценки прогресса», «Антропометрия», «Таймер отдыха» и т.п., использует токены `Spacing`, `CardShape`, `Volt`, `MaterialTheme.colorScheme`.
- `AndroidManifest.xml` уже содержит `POST_NOTIFICATIONS`.
- WorkManager в проекте **не подключён** — нужно добавить зависимость.

## Goals / Non-Goals

**Goals:**
- Хранить дату окончания абонемента (по умолчанию `null`).
- Показывать поле «Дата окончания абонемента» в настройках с выбором через `DatePickerDialog`, запрещая прошедшие даты.
- Раз в день (~12:00) проверять, попадает ли сегодня в окно `[expiry-6 .. expiry]`, и показывать одно уведомление в трее.
- Изолировать канал уведомлений от `active_workout`.
- Не спамить, если дата не указана или уже прошла.
- Переживать reboot устройства без дополнительных разрешений.

**Non-Goals:**
- Точные будильники со звуком/вибрацией в заданную минуту — не нужно (плавающее время в течение дня допустимо).
- Выбор «за сколько дней напоминать» — хардкод 7 дней.
- Несколько абонементов / история продлений — вне scope.
- Push-уведомления с сервера — только локальные.
- Напоминания после истечения срока — явно исключено пользователем.

## Decisions

### Решение 1: WorkManager с PeriodicWorkRequest(1 day)

**Выбор:** `WorkManager.enqueueUniquePeriodicWork` с `PeriodicWorkRequest` интервалом 1 день.

**Альтернативы:**
- `AlarmManager.setExactAndAllowWhileIdle` — даёт точное время, но требует `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` (Android 12+) и `BootReceiver` для перепланирования после ребута. Для «раз в день напомнить» — overkill.
- `JobScheduler` напрямую — более низкоуровневый, WorkManager его оборачивает и даёт единый API.

**Почему WorkManager:**
- Переживает reboot без `RECEIVE_BOOT_COMPLETED`.
- Не требует новых разрешений.
- Каноничный Jetpack-подход, хорошо ложится на существующий стек (Kotlin, coroutines).
- Плавающее время сработки приемлемо для напоминания о абонементе.

### Решение 2: Время срабатывания ~12:00 через initialDelay

**Выбор:** При планировании вычислить `initialDelay` до ближайшего 12:00 локального времени, затем `PeriodicWorkRequest` с интервалом 24 часа.

**Нюанс:** WorkManager не гарантирует точное время — сработки могут сдвигаться из-за doze/battery optimization. Это допустимо: уведомление всё равно появится в течение дня.

### Решение 3: Хранение как ISO-строка в DataStore

**Выбор:** `stringPreferencesKey("membership_expiry")`, значение в формате `LocalDate.toString()` (`"2026-08-31"`).

**Альтернатива:** три отдельных int-ключа (год/месяц/день) — больше кода, нет преимуществ.

**Почему:** DataStore Preferences не имеет встроенного типа для даты; ISO-строка однозначно парсится `LocalDate.parse(...)`.

### Решение 4: Окно напоминаний `[expiry-6 .. expiry]` (7 дней включительно)

**Выбор:** Уведомление показывается, если `daysLeft = ChronoUnit.DAYS.between(today, expiry)` находится в диапазоне `0..6` включительно.

**Согласовано с пользователем:** в день истечения (`daysLeft == 0`) тоже показываем. После истечения (`daysLeft < 0`) — молчим.

### Решение 5: Отдельный NotificationChannel `membership_reminder`

**Выбор:** Новый канал с `IMPORTANCE_DEFAULT` (виден в трее, со звуком/вибрацией по системным настройкам, не silent), `setShowBadge(true)`.

**Почему не `IMPORTANCE_HIGH`:** пользователь явно сказал «не будильник, как Duolingo». `IMPORTANCE_DEFAULT` — это обычный пуш, заметный, но не агрессивный.

**Почему не переиспользуем `active_workout`:** тот `IMPORTANCE_LOW` + `setSilent(true)` + `setShowBadge(false)` — противоположность тому, что нужно для напоминания. Каналы нельзя «поменять на лету» после создания, а смешивать типы уведомлений в одном канале — плохая практика (пользователь не сможет отдельно заглушить напоминания).

### Решение 6: Перепланирование при изменении даты

**Выбор:** Подписка на `membershipExpiryDate` в `GymProgressApp` (или в `WorkoutViewModel.init`):
- `null` → `WorkManager.cancelUniqueWork(WORK_NAME)`.
- `not null` → `enqueueUniquePeriodicWork(WORK_NAME, REPLACE, request)`.

**Почему `REPLACE`:** при изменении даты нужно пересчитать `initialDelay` до ближайшего 12:00. `KEEP` не обновит задержку.

### Решение 7: Worker сам читает дату из SettingsRepository

**Выбор:** `MembershipReminderWorker.doWork()` читает `membershipExpiryDate` через `SettingsRepository(applicationContext)` (первое значение Flow через `.first()`).

**Почему:** Дата может измениться между планированием и сработкой. Worker всегда работает с актуальным значением, а не с snapshot'ом на момент планирования. Это упрощает логику и устраняет рассинхрон.

### Решение 8: UI — DatePickerDialog с selectableDates

**Выбор:** Переиспользовать паттерн из `AddEntryDialog.kt`, добавить `selectableDates` фильтр, запрещающий даты раньше сегодня.

**Альтернатива:** текстовое поле с парсингом — уже есть для веса/роста, но для даты хуже (форматы, ошибки ввода). DatePicker надёжнее и соответствует запросу пользователя «просто календарик».

## Risks / Trade-offs

- **[WorkManager может задерживать сработки на часы]** → Mitigation: допустимо для напоминания о абонементе; пользователь не ожидает точности до минуты. Если станет проблемой — можно перейти на `AlarmManager` позже.
- **[Пользователь может не выдать `POST_NOTIFICATIONS` на Android 13+]** → Mitigation: Worker оборачивает `notify(...)` в try/catch `SecurityException` (как уже сделано в `ActiveWorkoutService`), молча пропускает. Дата остаётся сохранённой, при выдаче разрешения следующие сработки начнут показывать уведомления.
- **[Дата в прошлом после выбора]** → Mitigation: `DatePickerDialog` запрещает прошедшие даты через `selectableDates`. Дополнительно Worker проверяет `daysLeft < 0` и молчит — защита от системных сдвигов времени.
- **[Часовой пояс]** → Mitigation: все вычисления через `LocalDate.now(ZoneId.systemDefault())` и `LocalDate.parse(...)`. `DatePicker` уже возвращает millis в системном TZ (как в `AddEntryDialog`).
- **[Канал создаётся при первой сработке Worker'а]** → Mitigation: `ensureNotificationChannel()` в начале `doWork()`, идемпотентен (проверяет `getNotificationChannel != null`).
- **[Перепланирование при каждом изменении даты создаёт новый воркер]** → Mitigation: `enqueueUniquePeriodicWork` с `REPLACE` гарантирует один экземпляр по имени `WORK_NAME`.
- **[WorkManager dependency увеличивает APK]** → Mitigation: ~200 KB, приемлемо для функциональности, которую запросил пользователь.
