## 1. Зависимости и инфраструктура

- [x] 1.1 Добавить `androidx-work-runtime-ktx` в `gradle/libs.versions.toml` (версия + library declaration)
- [x] 1.2 Подключить зависимость в `app/build.gradle.kts`
- [x] 1.3 Собрать проект (`./gradlew :app:assembleDebug`) и убедиться, что WorkManager резолвится

## 2. Хранение даты в SettingsRepository

- [x] 2.1 Добавить `membershipExpiryKey = stringPreferencesKey("membership_expiry")` в `SettingsRepository`
- [x] 2.2 Добавить `val membershipExpiryDate: Flow<LocalDate?>` с парсингом ISO-строки через `LocalDate.parse(...)` (с try/catch на случай повреждённых данных)
- [x] 2.3 Добавить `suspend fun setMembershipExpiryDate(value: LocalDate?)` — сохраняет `value.toString()` или удаляет ключ при `null`
- [x] 2.4 Проверить, что Flow emits `null` по умолчанию (новая установка)

## 3. NotificationChannel и построение уведомления

- [x] 3.1 Создать `service/MembershipReminderNotifier.kt` с методами `ensureChannel(context)` и `showReminder(context, expiry, daysLeft)`
- [x] 3.2 В `ensureChannel`: создать `NotificationChannel("membership_reminder", "Напоминания о абонементе", IMPORTANCE_DEFAULT)` с `setShowBadge(true)`, идемпотентно (проверка `getNotificationChannel != null`)
- [x] 3.3 В `showReminder`: построить `NotificationCompat.Builder` с каналом `membership_reminder`, `smallIcon = R.mipmap.ic_launcher`, текстом «Абонемент заканчивается через N дней (D MMMM). Не забудь продлить!» (для `daysLeft == 0` — «...сегодня (D MMMM). Не забудь продлить!»), `setContentIntent` → `MainActivity` с `FLAG_ACTIVITY_SINGLE_TOP`
- [x] 3.4 Обернуть `NotificationManagerCompat.notify(...)` в try/catch `SecurityException` (как в `ActiveWorkoutService`)

## 4. MembershipReminderWorker

- [x] 4.1 Создать `service/MembershipReminderWorker.kt` — `CoroutineWorker`
- [x] 4.2 В `doWork()`: создать `SettingsRepository(applicationContext)`, прочитать `membershipExpiryDate.first()`
- [x] 4.3 Если `null` → вернуть `Result.success()` без уведомления
- [x] 4.4 Вычислить `daysLeft = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.systemDefault()), expiry)`
- [x] 4.5 Если `daysLeft < 0` или `daysLeft >= 7` → `Result.success()` без уведомления
- [x] 4.6 Если `daysLeft in 0..6` → вызвать `MembershipReminderNotifier.ensureChannel` + `showReminder`, вернуть `Result.success()`

## 5. Планировщик WorkManager

- [x] 5.1 Создать `service/MembershipReminderScheduler.kt` с методами `schedule(context)` и `cancel(context)`
- [x] 5.2 В `schedule`: вычислить `initialDelay` до ближайшего 12:00 локального времени, создать `PeriodicWorkRequestBuilder<MembershipReminderWorker>(24, TimeUnit.HOURS).setInitialDelay(...)`, вызвать `WorkManager.enqueueUniquePeriodicWork(WORK_NAME, REPLACE, request)`
- [x] 5.3 В `cancel`: `WorkManager.cancelUniqueWork(WORK_NAME)`
- [x] 5.4 Константа `WORK_NAME = "membership_reminder_daily"`

## 6. Интеграция в WorkoutViewModel

- [x] 6.1 Добавить в `WorkoutViewModel` поле `membershipExpiryDate: StateFlow<LocalDate?>` из `SettingsRepository`
- [x] 6.2 Добавить `fun onMembershipExpiryDateChanged(value: LocalDate?)` — вызывает `setMembershipExpiryDate` в `viewModelScope`
- [x] 6.3 В `init` подписаться на `membershipExpiryDate` и вызывать `MembershipReminderScheduler.schedule/cancel` при изменениях (только когда приложение в foreground, либо безусловно — WorkManager сам разрулит)

## 7. UI в SettingsScreen

- [x] 7.1 Добавить параметры `membershipExpiryDate: LocalDate?` и `onMembershipExpiryDateChanged: (LocalDate?) -> Unit` в сигнатуру `SettingsScreen`
- [x] 7.2 Добавить новый блок «Дата окончания абонемента» в `SettingsScreen` (после блока таймера отдыха или в логически подходящем месте), используя токены `Spacing`, `CardShape`, `MaterialTheme.colorScheme`, `Volt` для акцента
- [x] 7.3 Поле даты: при тапе открывает `showDatePicker = true`, отображает отформатированную дату через `FormatUtils.formatDate` или плейсхолдер «Не указано»
- [x] 7.4 Подсказка под полем: «За 7 дней до окончания придёт напоминание о продлении»
- [x] 7.5 Кнопка очистки даты (иконка `Close`/`Clear`) рядом с полем, вызывает `onMembershipExpiryDateChanged(null)`
- [x] 7.6 `DatePickerDialog` с `rememberDatePickerState` (initialSelectedDateMillis = сегодня), `selectableDates` фильтр запрещает даты `< today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()`
- [x] 7.7 На подтверждение: конвертировать `selectedDateMillis → LocalDate` (как в `AddEntryDialog`), вызвать `onMembershipExpiryDateChanged`

## 8. Прокидывание в GymProgressApp

- [x] 8.1 В `GymProgressApp` передать `membershipExpiryDate` и `onMembershipExpiryDateChanged` из `WorkoutViewModel` в `SettingsScreen`

## 9. Документация

- [x] 9.1 Обновить `docs/IMPROVEMENT_PLAN.md` — отметить выполненным пункт про напоминалки/уведомления (если есть) или добавить запись о реализации
- [x] 9.2 При необходимости обновить `docs/POTENTIAL_ERRORS_ANALYSIS.md` (если упоминались уведомления как техдолг)

## 10. Верификация

- [x] 10.1 `./gradlew :app:assembleDebug` — сборка без ошибок
- [x] 10.2 `./gradlew :app:lintDebug` — без новых критических warning'ов
- [ ] 10.3 Ручная проверка: открыть настройки → блок «Дата окончания абонемента» отображается, поле пустое
- [ ] 10.4 Ручная проверка: тап по полю → открывается DatePicker, прошедшие даты недоступны
- [ ] 10.5 Ручная проверка: выбрать дату через 3 дня → поле показывает дату, сохраняется в DataStore
- [ ] 10.6 Ручная проверка: кнопка очистки → поле пустое, `null` сохранён
- [ ] 10.7 Ручная проверка: установить дату в окне `[сегодня .. сегодня+6]`, дождаться сработки Worker'а (или триггернуть вручную через отладку) → уведомление появляется в трее
- [ ] 10.8 Ручная проверка: тап по уведомлению → открывается `MainActivity`
- [ ] 10.9 Ручная проверка: установить дату в прошлом (через отладку DataStore) → Worker молчит
- [ ] 10.10 Ручная проверка: очистить дату → `cancelUniqueWork` вызывается, новых уведомлений нет
