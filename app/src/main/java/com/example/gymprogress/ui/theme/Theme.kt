package com.example.gymprogress.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
// IRON CORE Design System — Theme
// ═══════════════════════════════════════════════════════════════════
// Суровый, функциональный. Electric Volt + Obsidian + Carbon.
// Material 3 dark/light schemes + extended tokens.
// ═══════════════════════════════════════════════════════════════════

// ─── Material 3 Color Schemes ───

private val DarkColorScheme = darkColorScheme(
    primary = Volt,
    onPrimary = Color.Black,
    primaryContainer = VoltContainer,
    onPrimaryContainer = Volt,
    secondary = Steel,
    onSecondary = Color.Black,
    secondaryContainer = SteelContainer,
    onSecondaryContainer = Steel,
    tertiary = Steel,
    onTertiary = Color.Black,
    tertiaryContainer = SteelContainer,
    onTertiaryContainer = Steel,
    error = ErrorRed,
    onError = Color.Black,
    errorContainer = Color(0xFF5C0000),
    onErrorContainer = ErrorRedLight,
    background = Obsidian,
    onBackground = TextPrimaryDark,
    surface = Carbon,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = DividerDark,
    inverseSurface = LightSurface,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = VoltDark,
    surfaceTint = Volt
)

private val LightColorScheme = lightColorScheme(
    primary = VoltDark,
    onPrimary = Color.White,
    primaryContainer = VoltContainerLight,
    onPrimaryContainer = VoltDark,
    secondary = DeepSteel,
    onSecondary = Color.White,
    secondaryContainer = SteelContainerLight,
    onSecondaryContainer = DeepSteel,
    tertiary = DeepSteel,
    onTertiary = Color.White,
    tertiaryContainer = SteelContainerLight,
    onTertiaryContainer = DeepSteel,
    error = ErrorRedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = ErrorRedDark,
    background = LightConcrete,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = DividerLight,
    inverseSurface = Carbon,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = Volt,
    surfaceTint = VoltDark
)

// ─── Extended color tokens (GymTheme.colors) ───

@Immutable
data class GymExtendedColors(
    val accent: Color,
    val accentDim: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val divider: Color,
    val surfaceHigh: Color,
    val textDisabled: Color,
    val muscleHighlight: Color,
    val muscleHighlightDim: Color,
    val muscleBody: Color,
    val muscleBodyDark: Color
)

private val DarkExtendedColors = GymExtendedColors(
    accent = Volt,
    accentDim = VoltDim,
    success = SuccessGreenLight,
    successContainer = Color(0xFF1B5E20),
    onSuccess = Color.Black,
    warning = WarningAmber,
    warningContainer = Color(0xFF5D4037),
    onWarning = Color.Black,
    divider = DividerDark,
    surfaceHigh = CarbonHigh,
    textDisabled = TextDisabledDark,
    muscleHighlight = MuscleHighlightPrimary,
    muscleHighlightDim = MuscleHighlightSecondary,
    muscleBody = MuscleBodyGrey,
    muscleBodyDark = MuscleBodyGreyDark
)

private val LightExtendedColors = GymExtendedColors(
    accent = VoltDark,
    accentDim = VoltDim,
    success = SuccessGreenDark,
    successContainer = Color(0xFFC8E6C9),
    onSuccess = Color.White,
    warning = WarningAmberDark,
    warningContainer = Color(0xFFFFF8E1),
    onWarning = Color.Black,
    divider = DividerLight,
    surfaceHigh = LightSurfaceHigh,
    textDisabled = TextDisabledLight,
    muscleHighlight = MuscleHighlightPrimary,
    muscleHighlightDim = MuscleHighlightSecondary,
    muscleBody = MuscleBodyGrey,
    muscleBodyDark = MuscleBodyGreyDark
)

val LocalGymExtendedColors = staticCompositionLocalOf { LightExtendedColors }

// ─── Convenience accessor ───

object GymTheme {
    val colors: GymExtendedColors
        @Composable get() = LocalGymExtendedColors.current
}

// ─── Theme composable ───

@Composable
fun GymProgressTheme(
    darkTheme: Boolean = true, // IRON CORE: dark by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalGymExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GymShapes,
            content = content
        )
    }
}