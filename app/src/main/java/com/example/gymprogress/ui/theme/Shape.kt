package com.example.gymprogress.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// IRON CORE Design System — Shapes
// ═══════════════════════════════════════════════════════════════════
// Принцип: острые углы, брутальные блоки. 4dp и 8dp максимум.
// Избегаем «таблеток» — карточки как металлические пластины.
// ═══════════════════════════════════════════════════════════════════

val GymShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

// ─── Specific shape tokens ───
val ButtonShape = RoundedCornerShape(8.dp)
val CardShape = RoundedCornerShape(8.dp)
val CardShapeSmall = RoundedCornerShape(4.dp)
val DialogShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(4.dp)
val TextFieldShape = RoundedCornerShape(8.dp)
val FabShape = RoundedCornerShape(8.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
val ImageShape = RoundedCornerShape(4.dp)
