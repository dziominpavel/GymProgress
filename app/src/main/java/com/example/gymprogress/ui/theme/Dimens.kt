package com.example.gymprogress.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// IRON CORE Design System — Spacing & Dimensions
// ═══════════════════════════════════════════════════════════════════
// Сетка: 8dp базовый шаг (стандарт Android).
// Padding: 16dp (Standard), 8dp (Tight), 24dp (Loose).
// ═══════════════════════════════════════════════════════════════════

object Spacing {
    val xxxs: Dp = 2.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp       // tight
    val sm: Dp = 12.dp
    val md: Dp = 16.dp      // standard
    val lg: Dp = 24.dp      // loose
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 56.dp
    val massive: Dp = 64.dp
}

// ─── Screen-level padding ───
object ScreenPadding {
    val horizontal: Dp = 16.dp
    val vertical: Dp = 16.dp
    val top: Dp = 16.dp
    val bottom: Dp = 80.dp   // FAB clearance
}

// ─── Component dimensions ───
object ComponentSize {
    // Buttons
    val buttonHeight: Dp = 48.dp
    val buttonHeightSmall: Dp = 36.dp
    val buttonHeightLarge: Dp = 56.dp
    val buttonMinWidth: Dp = 120.dp

    // Icons
    val iconXs: Dp = 16.dp
    val iconSm: Dp = 20.dp
    val iconMd: Dp = 24.dp
    val iconLg: Dp = 32.dp
    val iconXl: Dp = 48.dp

    // Avatar / circles
    val avatarSm: Dp = 32.dp
    val avatarMd: Dp = 40.dp
    val avatarLg: Dp = 52.dp
    val avatarXl: Dp = 64.dp

    // Cards
    val cardMinHeight: Dp = 64.dp

    // FAB
    val fabSize: Dp = 56.dp
    val fabSizeSmall: Dp = 40.dp

    // Divider
    val dividerThickness: Dp = 1.dp
    val accentBarWidth: Dp = 4.dp
    val accentBarHeight: Dp = 20.dp

    // MuscleGroupIcon
    val muscleIconDefault: Dp = 64.dp
    val muscleIconSmall: Dp = 40.dp
    val muscleIconLarge: Dp = 80.dp

    // Progress bar
    val progressBarHeight: Dp = 6.dp
    val progressBarHeightLarge: Dp = 10.dp

    // Dot indicator
    val dotSize: Dp = 8.dp
    val dotSizeSmall: Dp = 6.dp
}

// ─── Elevation levels ───
object Elevation {
    val none: Dp = 0.dp
    val low: Dp = 1.dp
    val medium: Dp = 3.dp
    val high: Dp = 6.dp
    val overlay: Dp = 8.dp
}
