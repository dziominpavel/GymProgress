# GymProgress — Design System «IRON CORE»

> Дизайн-система Android-приложения для трекинга силовых тренировок.
> Источник истины — реальные токены в `app/src/main/java/com/example/gymprogress/ui/theme/`.
> Этот документ только описывает их и даёт правила использования.
> Дата ревизии: 2026-04-25.

## Философия

«IRON CORE» — суровый, функциональный, без лишних эффектов. Металл, бетон, энергия.

- Тёмная тема — основная (по умолчанию). Светлая — реализована, но в текущей сборке зашита `darkTheme = true` (см. `IMPROVEMENT_PLAN.md` фаза 4.1).
- Единственный акцентный цвет — **Electric Volt** (`#D1FF00`). Считывается периферическим зрением. Никаких пастельных тонов, градиентов, неонов.
- Карточки и блоки — как «металлические пластины»: умеренное скругление 4–12dp, не «таблетки».
- 60% нейтральных поверхностей · 30% вторичных тонов · 10% акцентного Volt.

---

## 1. Цветовая палитра

Все токены определены в `ui/theme/Color.kt`.

### Primary — Electric Volt

| Токен Kotlin | HEX | Назначение |
|--------------|-----|------------|
| `Volt` | `#D1FF00` | Primary (dark theme), акцент, FAB, CTA |
| `VoltDark` | `#5A6E00` | Primary (light theme) |
| `VoltDim` | `#8FA800` | Dimmed акцент, иконки мышц (вторичная подсветка) |
| `VoltContainer` | `#1A2000` | `primaryContainer` (dark) |
| `VoltContainerLight` | `#E8FFB0` | `primaryContainer` (light) |
| `AccentOrange` | = `Volt` | Backward-compat alias, **не использовать в новом коде** |

### Secondary — Steel

| Токен | HEX | Назначение |
|-------|-----|------------|
| `Steel` | `#A8ABB4` | Secondary (dark) |
| `DeepSteel` | `#5D5E62` | Secondary (light) |
| `SteelContainer` | `#2A2A2E` | `secondaryContainer` (dark) |
| `SteelContainerLight` | `#E0E1E5` | `secondaryContainer` (light) |

### Semantic

| Токен | HEX | Назначение |
|-------|-----|------------|
| `SuccessGreen` | `#4CAF50` | Прогресс/PR (через `GymTheme.colors.success`) |
| `SuccessGreenLight` | `#81C784` | Light-вариант успеха |
| `SuccessGreenDark` | `#2E7D32` | Dark-вариант успеха |
| `WarningAmber` | `#FFC107` | Предупреждения |
| `WarningAmberDark` | `#F57F17` | Light-тема: warning |
| `ErrorRed` | `#FF4545` | Ошибки, удаление (Material `error` в dark) |
| `ErrorRedLight` | `#FF6B6B` | `onErrorContainer` (dark) |
| `ErrorRedDark` | `#B3261E` | Material `error` в light |

### Нейтральные поверхности

#### Dark — Obsidian / Carbon
| Токен | HEX | Material slot |
|-------|-----|----------------|
| `Obsidian` | `#0D0D0D` | `background` |
| `Carbon` | `#1A1A1A` | `surface` (карточки) |
| `CarbonVariant` | `#222222` | `surfaceVariant` |
| `CarbonHigh` | `#2A2A2A` | Диалоги, sheets (через `GymTheme.colors.surfaceHigh`) |
| `OutlineDark` | `#333333` | `outline` |

#### Light — Concrete
| Токен | HEX | Material slot |
|-------|-----|----------------|
| `LightConcrete` | `#F4F4F4` | `background` |
| `LightSurface` | `#FFFFFF` | `surface` |
| `LightSurfaceVariant` | `#EAEAEC` | `surfaceVariant` |
| `LightSurfaceHigh` | `#E0E0E2` | `GymTheme.colors.surfaceHigh` |
| `OutlineLight` | `#BBBBBB` | `outline` |

### Текст

| Назначение | Dark | Light |
|------------|------|-------|
| Primary | `TextPrimaryDark` `#ECECEC` | `TextPrimaryLight` `#121212` |
| Secondary | `TextSecondaryDark` `#9E9EA2` | `TextSecondaryLight` `#5D5E62` |
| Disabled | `TextDisabledDark` `#555558` | `TextDisabledLight` `#9E9E9E` |

### Дивайдеры и иконки мышц

| Токен | HEX | Назначение |
|-------|-----|------------|
| `DividerDark` | `#2A2A2E` | Разделители (dark) |
| `DividerLight` | `#DDDDDD` | Разделители (light) |
| `MuscleHighlightPrimary` | `#D1FF00` | Целевая мышца на схеме |
| `MuscleHighlightSecondary` | `#8FA800` | Вторичная мышца |
| `MuscleBodyGrey` | `#555558` | Тело силуэта |
| `MuscleBodyGreyDark` | `#444447` | Тёмный вариант |

---

## 2. Темы

Файл: `ui/theme/Theme.kt`.

```kotlin
@Composable
fun GymProgressTheme(
    darkTheme: Boolean = true, // IRON CORE: dark by default
    content: @Composable () -> Unit
)
```

- `MaterialTheme.colorScheme` — стандартные слоты (`primary`, `surface`, `error`, …).
- **Расширенные токены** доступны через `GymTheme.colors`:
  ```kotlin
  GymTheme.colors.accent          // Volt / VoltDark
  GymTheme.colors.accentDim       // VoltDim
  GymTheme.colors.success         // зелёный (PR, прогресс)
  GymTheme.colors.warning         // янтарный
  GymTheme.colors.divider         // разделители
  GymTheme.colors.surfaceHigh     // диалоги, sheets
  GymTheme.colors.textDisabled    // неактивный текст
  GymTheme.colors.muscleHighlight // подсветка мышц
  ```

**Текущее ограничение:** `darkTheme = true` зашит в коде, выбор пользователем не реализован. План — фаза 4.1 в `IMPROVEMENT_PLAN.md`.

---

## 3. Типографика

Файл: `ui/theme/Type.kt`. `FontFamily.Default` → Roboto на Android. Жирные заголовки, спокойный body, нулевой tracking в крупных стилях.

| Стиль | Вес | Размер | Высота | Tracking |
|-------|-----|--------|--------|----------|
| `displayLarge` | Black (900) | 57sp | 64sp | -0.25sp |
| `displayMedium` | Bold (700) | 45sp | 52sp | 0sp |
| `displaySmall` | Bold (700) | 36sp | 44sp | 0sp |
| `headlineLarge` | Bold (700) | 32sp | 40sp | 0sp |
| `headlineMedium` | Bold (700) | 28sp | 36sp | 0sp |
| `headlineSmall` | SemiBold (600) | 24sp | 32sp | 0sp |
| `titleLarge` | Bold (700) | 22sp | 28sp | 0sp |
| `titleMedium` | SemiBold (600) | 16sp | 24sp | 0.15sp |
| `titleSmall` | SemiBold (600) | 14sp | 20sp | 0.1sp |
| `bodyLarge` | Normal (400) | 16sp | 24sp | 0.5sp |
| `bodyMedium` | Normal (400) | 14sp | 20sp | 0.25sp |
| `bodySmall` | Normal (400) | 12sp | 16sp | 0.4sp |
| `labelLarge` | SemiBold (600) | 14sp | 20sp | 0.1sp |
| `labelMedium` | Medium (500) | 12sp | 16sp | 0.5sp |
| `labelSmall` | Medium (500) | 11sp | 16sp | 0.5sp |

### Когда что брать
- **Display** — крупные числа: рекорд, текущий вес в активной тренировке, score-таблицы.
- **Headline** — заголовки экранов.
- **Title** — заголовки карточек, секций.
- **Body** — основной контент, описания.
- **Label** — кнопки, чипы, бейджи, подписи под полями.

В коде используется через `MaterialTheme.typography.titleMedium` и т.п. Не определять `TextStyle` inline.

---

## 4. Сетка и отступы

Файл: `ui/theme/Dimens.kt`. База — **8dp**, допустимо использовать 4dp для микро-зазоров.

### `Spacing`

| Токен | Значение | Применение |
|-------|----------|------------|
| `Spacing.xxxs` | 2dp | Микро-зазоры |
| `Spacing.xxs` | 4dp | Между label и value |
| `Spacing.xs` | 8dp | Между элементами в ряду (tight) |
| `Spacing.sm` | 12dp | Внутри компонентов |
| `Spacing.md` | 16dp | Основной padding (standard) |
| `Spacing.lg` | 24dp | Между секциями (loose) |
| `Spacing.xl` | 32dp | Крупные отступы |
| `Spacing.xxl` | 40dp | Между блоками |
| `Spacing.xxxl` | 48dp | Hero-секции |
| `Spacing.huge` | 56dp | Верхний отступ экрана |
| `Spacing.massive` | 64dp | Крупные разделители |

### `ScreenPadding`

| Токен | Значение | Применение |
|-------|----------|------------|
| `ScreenPadding.horizontal` | 16dp | Боковой отступ экрана |
| `ScreenPadding.vertical` | 16dp | Верх/низ экрана |
| `ScreenPadding.top` | 16dp | Только верх |
| `ScreenPadding.bottom` | 80dp | Низ с учётом FAB |

### `ComponentSize`

| Токен | Значение | Применение |
|-------|----------|------------|
| `ComponentSize.buttonHeight` | 48dp | Стандартная кнопка |
| `ComponentSize.buttonHeightSmall` | 36dp | Компактная |
| `ComponentSize.buttonHeightLarge` | 56dp | CTA |
| `ComponentSize.buttonMinWidth` | 120dp | Минимальная ширина кнопки |
| `ComponentSize.iconXs` | 16dp | Бейджи |
| `ComponentSize.iconSm` | 20dp | Inline-иконки |
| `ComponentSize.iconMd` | 24dp | Стандарт, навигация |
| `ComponentSize.iconLg` | 32dp | Hero-секции |
| `ComponentSize.iconXl` | 48dp | Empty states |
| `ComponentSize.avatarSm/Md/Lg/Xl` | 32/40/52/64dp | Аватары и кружки |
| `ComponentSize.cardMinHeight` | 64dp | Минимальная высота карточки |
| `ComponentSize.fabSize` | 56dp | Стандартный FAB |
| `ComponentSize.fabSizeSmall` | 40dp | Small FAB |
| `ComponentSize.dividerThickness` | 1dp | Разделители |
| `ComponentSize.accentBarWidth` | 4dp | Вертикальная Volt-полоска у заголовков |
| `ComponentSize.accentBarHeight` | 20dp | Высота той же полоски |
| `ComponentSize.muscleIconDefault/Small/Large` | 64/40/80dp | Иконка мышц |
| `ComponentSize.progressBarHeight` | 6dp | Тонкий прогресс-бар |
| `ComponentSize.progressBarHeightLarge` | 10dp | Крупный |
| `ComponentSize.dotSize` | 8dp | Индикатор |
| `ComponentSize.dotSizeSmall` | 6dp | Мелкий индикатор |

### `Elevation`

| Токен | Значение | Назначение |
|-------|----------|------------|
| `Elevation.none` | 0dp | Flat карточки |
| `Elevation.low` | 1dp | Мягкое поднятие |
| `Elevation.medium` | 3dp | Стандартные карточки |
| `Elevation.high` | 6dp | Диалоги |
| `Elevation.overlay` | 8dp | Bottom sheets, оверлеи |

---

## 5. Формы

Файл: `ui/theme/Shape.kt`. Принцип IRON CORE — острые углы, минимум скруглений.

### Material 3 `Shapes`
| Уровень | Радиус |
|---------|--------|
| `small` | 4dp |
| `medium` | 8dp |
| `large` | 8dp |
| `extraLarge` | 12dp |

### Specific shape tokens
| Токен | Радиус | Применение |
|-------|--------|------------|
| `ButtonShape` | 8dp | Кнопки |
| `CardShape` | 8dp | Карточки |
| `CardShapeSmall` | 4dp | Малые карточки, чипы-блоки |
| `DialogShape` | 12dp | Диалоги |
| `ChipShape` | 4dp | Чипы / бейджи |
| `TextFieldShape` | 8dp | Поля ввода |
| `FabShape` | 8dp | FAB (квадратный, не круглый) |
| `BottomSheetShape` | 12dp top | Bottom sheet |
| `ImageShape` | 4dp | Изображения, превью |

> ⚠️ Не использовать `RoundedCornerShape(...)` inline в новом коде — только токены выше.

---

## 6. Иконки

- Базовый набор: `androidx.compose.material.icons.filled.*` для активных и `outlined.*` для inline.
- Размеры — через `ComponentSize.iconXs/Sm/Md/Lg/Xl`.
- Цвет: `MaterialTheme.colorScheme.onSurface` / `onSurfaceVariant` / `primary`.
- **Запрещено:** duotone, multicolor, кастомные эмодзи в качестве иконок UI (эмодзи в empty-states допустимы, см. ниже).

### Иконки табов
В `MainActivity.AppDestinations`:
- `JOURNAL` → `Icons.Default.DateRange`
- `STATS` → `Icons.Default.Star`
- `EXERCISES` → `Icons.Default.List`

> Замена на более семантичные (`TrendingUp` для прогресса, `FitnessCenter` для упражнений) запланирована в `IMPROVEMENT_PLAN.md` фаза 4.3.

---

## 7. Иллюстрации мышечных групп

Реализовано в `ui/components/MuscleGroupIcon.kt` через Canvas API.

- Силуэт тела — `MuscleBodyGrey` (`#555558`).
- Целевая группа — `MuscleHighlightPrimary` (Volt, `#D1FF00`).
- Вторичная подсветка — `MuscleHighlightSecondary` (`#8FA800`).
- Геометричный силуэт, не анатомический атлас.
- Highlight только целевая группа.
- Скруглённый контейнер `CardShapeSmall` (4dp).

| Размер | Применение |
|--------|------------|
| `ComponentSize.muscleIconSmall` (40dp) | Списки, dropdown |
| `ComponentSize.muscleIconDefault` (64dp) | Карточки групп |
| `ComponentSize.muscleIconLarge` (80dp) | Детальный просмотр |

---

## 8. Правила использования акцента (Volt)

### Где использовать `Volt` / `colorScheme.primary` / `GymTheme.colors.accent`
- **FAB** (основное действие).
- **CTA-кнопки** (сохранить, создать, добавить).
- **Активный таб** в `NavigationSuiteScaffold`.
- **Прогресс-бары** (например, индикатор тренировки).
- **Accent-bar** — вертикальная полоска `4×20dp` у заголовков секций (`ComponentSize.accentBarWidth/Height`).
- **PR-бейджи** (личные рекорды, после фазы 2.3 плана).
- **Выделение максимального веса** в статистике.

### Где НЕ использовать
- Фон экранов или больших секций.
- Основной текст (только для интерактивных элементов).
- Более 2 акцентных элементов одновременно на экране.
- Декоративные элементы без функции.

### Правило 60-30-10
- **60%** — нейтральные поверхности (`background`, `surface`).
- **30%** — вторичные (`surfaceVariant`, `outline`).
- **10%** — акцентный Volt.

---

## 9. Состояния экранов

⚠️ **На момент ревизии** общих компонентов `GymEmptyState` / `GymLoadingState` / `GymErrorState` / `GymSkeletonList` **в коде нет** — каждый экран реализует пустое состояние по-своему (часто прямо в `Box` с `Text`). Унифицировать их планируется в `IMPROVEMENT_PLAN.md` фаза 2.4.

Целевые требования к будущему `EmptyState`:
- Иконка `ComponentSize.iconXl` (48dp).
- Заголовок `titleMedium`, цвет `onSurfaceVariant`.
- Подзаголовок `bodyMedium`, цвет `onSurfaceVariant`.
- Опциональная CTA-кнопка `OutlinedButton`.
- Никаких эмодзи в кастомном виде — только Material-иконки.

Для error-состояний:
- Material-иконка ошибки или `Icons.Default.ErrorOutline`.
- `colorScheme.error` для иконки и заголовка.
- Кнопка «Повторить» как `OutlinedButton`.

---

## 10. Компоненты — фактическое состояние

В `ui/components/` сейчас только `MuscleGroupIcon.kt`. Все экраны строят свои композиции напрямую из Material-примитивов (`Card`, `Button`, `OutlinedTextField`, `FloatingActionButton`).

Это **сознательное упрощение** на текущей стадии: компоненты «GymCard», «GymPrimaryButton» и т.п. не введены. План — извлечь повторяющиеся блоки в `ui/components/` по мере роста дублирования (см. `IMPROVEMENT_PLAN.md` фазы 2 и 4).

### Рекомендации до появления общих компонентов
- Карточки строить как `Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))`.
- FAB: `FloatingActionButton(shape = FabShape, containerColor = Volt, contentColor = MaterialTheme.colorScheme.onPrimary)` (как в `JournalScreen`).
- Кнопки: `Button(shape = ButtonShape)`, `OutlinedButton(shape = ButtonShape)`, `TextButton`.
- Поля ввода: `OutlinedTextField(shape = TextFieldShape)`.

---

## 11. Файловая структура темы

```
ui/
├── theme/
│   ├── Color.kt    — цветовые токены (Volt, Steel, Obsidian, Carbon, …)
│   ├── Type.kt     — типографика (Material 3 Typography с Roboto)
│   ├── Shape.kt    — Material 3 Shapes + специфичные токены
│   ├── Dimens.kt   — Spacing / ScreenPadding / ComponentSize / Elevation
│   └── Theme.kt    — GymProgressTheme + GymTheme.colors (extended)
└── components/
    └── MuscleGroupIcon.kt — Canvas-иллюстрации мышечных групп
```

---

## 12. Адаптивность

- **Навигация:** `NavigationSuiteScaffold` (Material 3) — автоматически переключается между bottom navigation на телефоне и `NavigationRail`/`PermanentDrawer` на широких экранах. Двухпанельные раскладки контента пока не реализованы — план в `IMPROVEMENT_PLAN.md` фаза 7.5.
- **Списки:** `LazyColumn` с `Arrangement.spacedBy(Spacing.xs)` или `Spacing.sm`.
- **Большой шрифт:** все размеры в `sp` для текста, `dp` для отступов. Проверка `fontScale = 1.3` пока не пройдена системно.

---

## 13. Чеклист код-ревью UI

- [ ] Цвета только из `MaterialTheme.colorScheme` или `GymTheme.colors`. Никаких `Color(0xFF...)` inline.
- [ ] Отступы только из `Spacing` / `ScreenPadding` / `ComponentSize`. Никаких `.dp` inline в новых компонентах.
- [ ] Формы только из shape-токенов (`CardShape`, `ButtonShape`, `FabShape`, …).
- [ ] Шрифт только через `MaterialTheme.typography.*`.
- [ ] Иконки — Material-набор; размер из `ComponentSize.icon*`.
- [ ] Тач-таргет ≥ 48dp (см. `ComponentSize.buttonHeight`).
- [ ] Все кликабельные `Icon` имеют `contentDescription`.
- [ ] Сортировка списков записей: `date ASC, id ASC` (правило проекта).
