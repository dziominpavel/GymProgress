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
- Версия: `version.properties`; при assemble/install/bundle автоматически увеличивается patch

### Документация (обязательно учитывать)
- `docs/DESIGN_SYSTEM.md` — дизайн-система IRON CORE: единственный акцент — **Electric Volt** (`#D1FF00`) на тёмной палитре Obsidian/Carbon, типографика Roboto, токены `Spacing`/`Dimens`/`CardShape`/`FabShape`/`ButtonShape`. Общих компонентов `GymCard`/`GymPrimaryButton` и т.п. пока нет — собирать из Material-примитивов.
- `docs/TRAINING_SCORING_REFERENCE.md` — две системы скоринга: упрощённая (E1RM по Epley/Brzycki + лестница усилия + бонус за подтверждение) и усложнённая (composite stimulus / E1RM / volume по цели).
- `docs/POTENTIAL_ERRORS_ANALYSIS.md` — текущий технический долг: нет FK на `Exercise`, case-sensitive уникальность имён, пустые правила Auto Backup, мёртвый код `exerciseNames`, активная тренировка без foreground service.
- `docs/IMPROVEMENT_PLAN.md` — общий план улучшений (бэкап, UX, графики, дизайн, навигация, a11y, техдолг).

## Секреты
- `OPENROUTER_API_KEY` — из `local.properties`, попадает в `BuildConfig`; для AI-советов в Тренере.

## Перед изменениями
1. **Сверяйся с документацией:** при работе с UI — `docs/DESIGN_SYSTEM.md`, со скорингом — `docs/TRAINING_SCORING_REFERENCE.md`, с известными проблемами — `docs/POTENTIAL_ERRORS_ANALYSIS.md`, общий вектор — `docs/IMPROVEMENT_PLAN.md`.
2. **Версии и сборка:** зависимости из `gradle/libs.versions.toml`; версия приложения в `version.properties`; при assemble/install/bundle patch увеличивается автоматически.

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
