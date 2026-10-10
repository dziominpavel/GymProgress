# GymProgress — правила для AI-агента

Единый IDE-нейтральный источник always-on правил. Читается любым агентом (Cursor, Devin Desktop, Devin CLI и др.). Дополнительные glob-правила лежат в `.cursor/rules/*.mdc` (compose-ui, data-layer, kotlin-android) и активируются по шаблонам файлов.

## Контекст проекта
Android-приложение на **Kotlin** для учёта силовых тренировок: журнал записей, справочник упражнений, статистика прогресса, встроенный «Тренер» с рекомендациями и опциональные советы через OpenRouter API.

### Стек
- **UI:** Jetpack Compose, Material 3, Adaptive Navigation Suite (NavigationSuiteScaffold)
- **Данные:** Room (KSP), DataStore Preferences
- **Архитектура:** один модуль `:app`, один `WorkoutViewModel`, ручная навигация (без Navigation Component) — состояние экранов в `GymProgressApp` через `rememberSaveable` и флаги

### Ключевые пути
- Точка входа: `MainActivity` → `GymProgressApp(viewModel)`
- Экраны: `app/src/main/java/com/example/gymprogress/ui/screens/*.kt`
- Тема: `ui/theme/` (Color/Type/Shape/Dimens/Theme); компоненты: `ui/components/` (`MuscleGroupIcon`, `EmptyState`, `HapticHelper`, `RestTimerFeedback`)
- Данные: `data/` — Room (AppDatabase, WorkoutDao, ExerciseDao), SettingsRepository, TrainerRecommendationEngine, две системы скоринга (`SimplifiedScoreCalculator`, `WorkoutScoreCalculator`) поверх `ScoringEngine`, AiService
- Версия: файл `version` (меняет только release-скрипт); автобампа при сборке нет

### Документация (обязательно учитывать)
- `docs/DESIGN_SYSTEM.md` — дизайн-система IRON CORE: единственный акцент — **Electric Volt** (`#D1FF00`) на тёмной палитре Obsidian/Carbon, типографика Roboto, токены `Spacing`/`Dimens`/`CardShape`/`FabShape`/`ButtonShape`. Общих компонентов `GymCard`/`GymPrimaryButton` и т.п. пока нет — собирать из Material-примитивов.
- `docs/TRAINING_SCORING_REFERENCE.md` — две системы скоринга: упрощённая (E1RM по Epley/Brzycki + лестница усилия + бонус за подтверждение) и усложнённая (composite stimulus / E1RM / volume по цели).
- `docs/POTENTIAL_ERRORS_ANALYSIS.md` — текущий технический долг: нет FK на `Exercise`, case-sensitive уникальность имён, пустые правила Auto Backup, мёртвый код `exerciseNames`, активная тренировка без foreground service.
- `docs/IMPROVEMENT_PLAN.md` — общий план улучшений (бэкап, UX, графики, дизайн, навигация, a11y, техдолг).

## Секреты
- `OPENROUTER_API_KEY` — из `local.properties`, попадает в `BuildConfig`; для AI-советов в Тренере.

## Перед изменениями
1. **Сверяйся с документацией:** при работе с UI — `docs/DESIGN_SYSTEM.md`, со скорингом — `docs/TRAINING_SCORING_REFERENCE.md`, с известными проблемами — `docs/POTENTIAL_ERRORS_ANALYSIS.md`, общий вектор — `docs/IMPROVEMENT_PLAN.md`.
2. **Версии и сборка:** зависимости из `gradle/libs.versions.toml`; версия приложения в файле `version` — автобампа при assemble/install/bundle нет.

## Приоритеты при доработках
- Не ломать существующую навигацию и единственный ViewModel: экраны получают данные и колбэки из `WorkoutViewModel`.
- При изменении схемы Room — добавлять миграцию в `AppDatabase`, не использовать destructive migration в release.
- Новый UI — только через токены дизайн-системы (`MaterialTheme.colorScheme`, `GymTheme.colors`, `Spacing`, `Dimens`, `CardShape`, `FabShape`, `ButtonShape`). Не вводить inline `Color(0xFF...)`, `.dp` и `RoundedCornerShape(...)` в новых местах.
- Ошибки БД и сети обрабатывать и показывать пользователю (Snackbar/Toast), не глотать исключения.

## Сортировка списков записей (важно)
- Списки записей тренировок (WorkoutEntry) для пользователя везде в одном порядке: **сверху первые по дате (старые), ниже — следующие**. То есть `date ASC`, затем `id ASC` (в пределах дня — сначала введённые). Проверять при любых новых экранах и списках: Прогресс (Упражнение, Дата), История, Журнал, диалоги.

## Планы
- **Все планы** создаются и обновляются в папке проекта: `.cursor/plans/` (от корня репозитория). Имя файла — короткое осмысленное на латинице, например `progress_date_tab.plan.md`.
- При обновлении — редактировать существующий файл плана (добавить раздел «Ответы», зафиксировать решения), не создавать второй файл.
- Единственный источник истины по плану — файл в `.cursor/plans/`.

## Структура ответов
- Код на Kotlin в стиле проекта: корутины, StateFlow, Compose.
- Комментарии и сообщения пользователю на русском, код и имена — на английском.
- При предложении рефакторинга учитывать `docs/POTENTIAL_ERRORS_ANALYSIS.md` (даты, миграции, целостность данных, дубликаты, обработка ошибок).

## OpenSpec (spec-driven development)
Проект инициализирован под OpenSpec. Структура: `openspec/` (`specs/`, `changes/`, `changes/archive/`). Скиллы и slash-команды установлены для Cursor (`.cursor/skills/openspec-*`, `.cursor/commands/opsx-*.md`) и Devin Desktop (`.windsurf/skills/openspec-*`, `.windsurf/workflows/opsx-*.md`).

### Workflow
- `/opsx:propose <idea>` — создать change с артефактами (proposal.md, design.md, tasks.md, specs/).
- `/opsx:apply` — реализовать задачи из текущего change по спеке.
- `/opsx:archive` — заархивировать завершённый change и обновить основные specs.
- `/opsx:explore` — исследовать кодовую базу перед предложением.

### Когда использовать
- Новые фичи и нетривиальные изменения — через OpenSpec (propose → review → apply → archive).
- Мелкие правки (опечатка, точечный багфикс) — можно напрямую без OpenSpec.
- Спеки и changes лежат в `openspec/` и коммитятся в git вместе с кодом.
- 

<!-- versioning:begin -->
## Версии и changelog (обязательно)

- Версия проекта меняется **только в момент релиза**: бамп вне релиза запрещён,
  между релизами номер остаётся номером последнего релиза. У статического трека
  версия заморожена и не меняется вовсе.
- В changelog попадают **только глобальные доработки** — новая функция, заметное
  изменение поведения, фикс, который пользователь реально видит. Пункт пишется
  в секцию `## [Unreleased]` в момент работы, а не перед релизом. Мелочь
  (косметика UI: отступы, цвета, выравнивание, подпись элемента, и локальные
  правки без заметного эффекта), как и чистые доки, спеки, тесты, CI,
  внутренний рефактор, в changelog не пишется.
- Релиз (где есть конвейер): `release.ps1 -Prepare` → сборка артефактов в `dist/`
  → `release.ps1`. Порядок не меняется.
- Секция changelog начинается с **пользовательского блока** — жирные метки
  категорий (`**Добавлено**`, `**Исправлено**`) и буллеты, одна строка = одно
  изменение, строка ≤140 символов, без `` ` `` — **выше первой строки** `###`;
  под категориями (`### Добавлено`, `### Исправлено` и аналоги) идут
  инженерские детали: change-id, состав изменений, тесты, метрики. Блок пиши
  **в момент работы**, вместе с пунктами под `###`, а не перед релизом.
  Подготовка релиза откажет, если блока нет или он вне формата, — и напечатает
  текст с пометкой «это увидят пользователи».
- Заметки GitHub-Release берутся из этого блока автоматически — отдельное
  описание релиза писать не нужно и нельзя дублировать.
- Сверка перед работой и после: `python scripts/check-version.py`
  (exit 0 — версия и changelog согласованы, exit 1 — рассинхрон или
  нарушение формат-контракта блока).
- Полные правила и запреты: `docs/versioning.md`.
<!-- versioning:end -->

<!-- commit-hygiene:begin -->
## Коммиты: без ИИ-трейлеров (обязательно)

- **ЗАПРЕЩЕНО** добавлять в сообщение коммита строки `Generated with [...]`,
  `Co-Authored-By` и любые другие авторские/агентские трейлеры.
- Перед коммитом проверь сообщение: в нём не должно быть строк, начинающихся
  с `Generated with` или `Co-Authored-By:`.
- Нарушение даёт только **начало строки**: обсуждение самого запрета, цитата
  правила или название файла внутри предложения трейлером не считаются.
<!-- commit-hygiene:end -->
