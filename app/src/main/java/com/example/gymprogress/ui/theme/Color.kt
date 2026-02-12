package com.example.gymprogress.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
// IRON CORE Design System — The Forge Palette
// ═══════════════════════════════════════════════════════════════════
// Суровый, функциональный дизайн. Металл, бетон, энергия.
// Акцент: Electric Volt — считывается периферическим зрением.
// ═══════════════════════════════════════════════════════════════════

// ─── Primary: Electric Volt — энергия, прогресс, CTA ───
val Volt = Color(0xFFD1FF00)              // dark theme primary
val VoltDark = Color(0xFF5A6E00)          // light theme primary
val VoltDim = Color(0xFF8FA800)           // dimmed volt for containers
val VoltContainer = Color(0xFF1A2000)     // dark primaryContainer
val VoltContainerLight = Color(0xFFE8FFB0) // light primaryContainer

// ─── Secondary: Steel — нейтральный, функциональный ───
val Steel = Color(0xFFA8ABB4)             // dark secondary
val DeepSteel = Color(0xFF5D5E62)         // light secondary
val SteelContainer = Color(0xFF2A2A2E)    // dark secondaryContainer
val SteelContainerLight = Color(0xFFE0E1E5) // light secondaryContainer

// ─── Semantic colors ───
val SuccessGreen = Color(0xFF4CAF50)
val SuccessGreenLight = Color(0xFF81C784)
val SuccessGreenDark = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFFFC107)
val WarningAmberDark = Color(0xFFF57F17)
val ErrorRed = Color(0xFFFF4545)          // Warning Red
val ErrorRedLight = Color(0xFFFF6B6B)
val ErrorRedDark = Color(0xFFB3261E)

// ─── Neutral palette — Dark theme (Obsidian + Carbon) ───
val Obsidian = Color(0xFF0D0D0D)          // background
val Carbon = Color(0xFF1A1A1A)            // surface (cards)
val CarbonVariant = Color(0xFF222222)     // surfaceVariant
val CarbonHigh = Color(0xFF2A2A2A)        // dialogs, sheets
val OutlineDark = Color(0xFF333333)       // borders

// ─── Neutral palette — Light theme (Concrete) ───
val LightConcrete = Color(0xFFF4F4F4)     // background
val LightSurface = Color(0xFFFFFFFF)      // surface
val LightSurfaceVariant = Color(0xFFEAEAEC) // surfaceVariant
val LightSurfaceHigh = Color(0xFFE0E0E2)
val OutlineLight = Color(0xFFBBBBBB)

// ─── On-colors (text / icons) ───
val TextPrimaryDark = Color(0xFFECECEC)   // high contrast on dark
val TextSecondaryDark = Color(0xFF9E9EA2) // muted on dark
val TextDisabledDark = Color(0xFF555558)
val TextPrimaryLight = Color(0xFF121212)
val TextSecondaryLight = Color(0xFF5D5E62)
val TextDisabledLight = Color(0xFF9E9E9E)

// ─── Dividers & borders ───
val DividerDark = Color(0xFF2A2A2E)
val DividerLight = Color(0xFFDDDDDD)

// ─── MuscleGroupIcon — Blueprint style ───
val MuscleHighlightPrimary = Color(0xFFD1FF00)  // Volt highlight
val MuscleHighlightSecondary = Color(0xFF8FA800) // dimmed volt
val MuscleBodyGrey = Color(0xFF555558)    // dark schematic body
val MuscleBodyGreyDark = Color(0xFF444447)

// ─── Backward compat aliases ───
val AccentOrange = Volt