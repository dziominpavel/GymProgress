## Why

Пользователи тренируются по абонементам с фиксированным сроком действия. Когда абонемент заканчивается, доступ в зал прекращается, а продлить его нужно успеть заранее. Сейчас в приложении нет никакого механизма напоминаний о сроке абонемента — пользователь узнаёт о просрочке уже у администратора зала. Нужно мягкое ежедневное напоминание в системном трее (по образцу Duolingo), которое стартует за 7 дней до окончания и не спамит, если дата не указана или уже прошла.

## What Changes

- В настройках появляется поле **«Дата окончания абонемента»** (по умолчанию пустое).
- Ввод даты — через Material 3 `DatePickerDialog` (как уже сделано в `AddEntryDialog` для даты тренировки). Запрещён выбор прошедших дат.
- Новое хранилище в `SettingsRepository`: `membershipExpiryDate: Flow<LocalDate?>` + `setMembershipExpiryDate(LocalDate?)`, persisted as ISO-строка в DataStore.
- Новая подсистема планирования на `WorkManager`: `PeriodicWorkRequest` с интервалом 1 день, перепланируется при изменении даты, отменяется при очистке.
- Новый `CoroutineWorker` (`MembershipReminderWorker`), который раз в день проверяет окно `[expiry-6 .. expiry]` (включая день истечения) и показывает одно уведомление в трее.
- Новый `NotificationChannel` `membership_reminder` с `IMPORTANCE_DEFAULT` (не silent, но без будильника — обычный пуш, как Duolingo). Изолирован от существующего канала `active_workout`.
- Тап по уведомлению открывает `MainActivity`.
- Текст уведомления: «Абонемент заканчивается через N дней (31 августа). Не забудь продлить!»
- Время срабатывания: ~12:00 дня (через `initialDelay` до ближайшего полудня).
- Новая зависимость: `androidx.work:work-runtime-ktx` в `libs.versions.toml` и `build.gradle.kts`.
- Если дата не указана (`null`) — воркер не планируется, ноль уведомлений.
- Если дата прошла — уведомления не показываются.

## Capabilities

### New Capabilities
- `membership-reminder`: хранение даты окончания абонемента, ежедневная проверка и показ уведомления в системном трее за 7 дней до истечения (включая день истечения), с возможностью отключения через очистку даты.

### Modified Capabilities
<!-- Нет существующих specs в openspec/specs/ — изменять нечего. -->

## Impact

- **Код**:
  - `data/SettingsRepository.kt` — новый ключ `membershipExpiryDate`, Flow + setter.
  - `ui/screens/SettingsScreen.kt` — новый блок «Дата окончания абонемента» с `DatePickerDialog`.
  - `viewmodel/WorkoutViewModel.kt` — прокидывание состояния и колбэка в `SettingsScreen`.
  - Новый файл `service/MembershipReminderWorker.kt` (CoroutineWorker).
  - Новый файл `service/MembershipReminderScheduler.kt` (enqueue/cancel WorkManager).
  - Новый файл `service/MembershipReminderNotifier.kt` (создание канала + построение уведомления) — либо методы внутри Worker'а.
- **Зависимости**: добавление `androidx.work:work-runtime-ktx` в `gradle/libs.versions.toml` и `app/build.gradle.kts`.
- **Манифест**: разрешение `POST_NOTIFICATIONS` уже есть. Новых разрешений не требуется (WorkManager не требует `SCHEDULE_EXACT_ALARM` / `RECEIVE_BOOT_COMPLETED` — переживает reboot сам).
- **Документация**: обновить `docs/IMPROVEMENT_PLAN.md` (раздел про уведомления/напоминалки) и при необходимости `docs/POTENTIAL_ERRORS_ANALYSIS.md`.
- **Дизайн-система**: новый UI в настройках — только через токены (`Spacing`, `CardShape`, `Volt`, `MaterialTheme.colorScheme`), без inline `Color(0xFF...)` / `.dp` / `RoundedCornerShape(...)`.
