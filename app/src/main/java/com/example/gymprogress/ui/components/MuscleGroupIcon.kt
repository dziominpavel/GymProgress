package com.example.gymprogress.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymprogress.ui.theme.MuscleBodyGrey
import com.example.gymprogress.ui.theme.MuscleBodyGreyDark
import com.example.gymprogress.ui.theme.MuscleHighlightPrimary
import com.example.gymprogress.ui.theme.MuscleHighlightSecondary

private val BodyGrey = MuscleBodyGrey
private val BodyGreyDark = MuscleBodyGreyDark
private val MuscleHighlight = MuscleHighlightPrimary
private val MuscleHighlightLight = MuscleHighlightSecondary

@Composable
fun MuscleGroupIcon(
    muscleGroup: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    backgroundColor: Color = Color(0xFF1A1A1A)
) {
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
    ) {
        val w = this.size.width
        val h = this.size.height

        when (muscleGroup) {
            "CHEST" -> drawChestIcon(w, h)
            "BACK" -> drawBackIcon(w, h)
            "SHOULDERS" -> drawShouldersIcon(w, h)
            "BICEPS" -> drawBicepsIcon(w, h)
            "TRICEPS" -> drawTricepsIcon(w, h)
            "LEGS" -> drawLegsIcon(w, h)
            "ABS" -> drawAbsIcon(w, h)
            "GLUTES" -> drawGlutesIcon(w, h)
            "FOREARMS" -> drawForearmsIcon(w, h)
            "CARDIO" -> drawCardioIcon(w, h)
            "OTHER" -> drawOtherIcon(w, h)
            else -> drawOtherIcon(w, h)
        }
    }
}

// ─── Shared body drawing helpers ───

private fun DrawScope.drawHead(w: Float, h: Float, color: Color = BodyGrey) {
    drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.10f))
}

private fun DrawScope.drawNeck(w: Float, h: Float, color: Color = BodyGrey) {
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.44f, h * 0.16f),
        size = Size(w * 0.12f, h * 0.06f),
        cornerRadius = CornerRadius(w * 0.03f)
    )
}

private fun DrawScope.drawTorso(w: Float, h: Float, color: Color = BodyGrey) {
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.22f)
        lineTo(w * 0.72f, h * 0.22f)
        lineTo(w * 0.68f, h * 0.58f)
        lineTo(w * 0.32f, h * 0.58f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawUpperArms(w: Float, h: Float, color: Color = BodyGrey) {
    // Left upper arm
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.12f, h * 0.23f),
        size = Size(w * 0.14f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
    // Right upper arm
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.74f, h * 0.23f),
        size = Size(w * 0.14f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
}

private fun DrawScope.drawForearms(w: Float, h: Float, color: Color = BodyGrey) {
    // Left forearm
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.09f, h * 0.41f),
        size = Size(w * 0.12f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
    // Right forearm
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.79f, h * 0.41f),
        size = Size(w * 0.12f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
}

private fun DrawScope.drawUpperLegs(w: Float, h: Float, color: Color = BodyGrey) {
    // Left upper leg
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.30f, h * 0.59f),
        size = Size(w * 0.17f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.07f)
    )
    // Right upper leg
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.53f, h * 0.59f),
        size = Size(w * 0.17f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.07f)
    )
}

private fun DrawScope.drawLowerLegs(w: Float, h: Float, color: Color = BodyGrey) {
    // Left lower leg
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.32f, h * 0.79f),
        size = Size(w * 0.13f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
    // Right lower leg
    drawRoundRect(
        color,
        topLeft = Offset(w * 0.55f, h * 0.79f),
        size = Size(w * 0.13f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
}

private fun DrawScope.drawFullBody(w: Float, h: Float) {
    drawHead(w, h)
    drawNeck(w, h)
    drawTorso(w, h)
    drawUpperArms(w, h)
    drawForearms(w, h)
    drawUpperLegs(w, h)
    drawLowerLegs(w, h)
}

// ─── Muscle group specific icons ───

private fun DrawScope.drawChestIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted chest / pecs
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.29f, h * 0.23f),
        size = Size(w * 0.19f, h * 0.14f)
    )
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.52f, h * 0.23f),
        size = Size(w * 0.19f, h * 0.14f)
    )
    // Inner detail lines
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.33f, h * 0.26f),
        size = Size(w * 0.12f, h * 0.08f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.55f, h * 0.26f),
        size = Size(w * 0.12f, h * 0.08f)
    )
}

private fun DrawScope.drawBackIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted back (lats + traps)
    // Traps
    val trapsPath = Path().apply {
        moveTo(w * 0.38f, h * 0.20f)
        lineTo(w * 0.62f, h * 0.20f)
        lineTo(w * 0.56f, h * 0.32f)
        lineTo(w * 0.44f, h * 0.32f)
        close()
    }
    drawPath(trapsPath, MuscleHighlight)
    // Lats
    val leftLat = Path().apply {
        moveTo(w * 0.30f, h * 0.28f)
        lineTo(w * 0.44f, h * 0.28f)
        lineTo(w * 0.42f, h * 0.50f)
        lineTo(w * 0.34f, h * 0.50f)
        close()
    }
    drawPath(leftLat, MuscleHighlight)
    val rightLat = Path().apply {
        moveTo(w * 0.56f, h * 0.28f)
        lineTo(w * 0.70f, h * 0.28f)
        lineTo(w * 0.66f, h * 0.50f)
        lineTo(w * 0.58f, h * 0.50f)
        close()
    }
    drawPath(rightLat, MuscleHighlight)
    // Detail
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.32f, h * 0.32f),
        size = Size(w * 0.10f, h * 0.14f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.58f, h * 0.32f),
        size = Size(w * 0.10f, h * 0.14f)
    )
}

private fun DrawScope.drawShouldersIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted deltoids
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.14f, h * 0.20f),
        size = Size(w * 0.16f, h * 0.12f)
    )
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.70f, h * 0.20f),
        size = Size(w * 0.16f, h * 0.12f)
    )
    // Detail highlights
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.17f, h * 0.22f),
        size = Size(w * 0.10f, h * 0.07f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.73f, h * 0.22f),
        size = Size(w * 0.10f, h * 0.07f)
    )
}

private fun DrawScope.drawBicepsIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted biceps (front of upper arm)
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.13f, h * 0.25f),
        size = Size(w * 0.12f, h * 0.15f)
    )
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.75f, h * 0.25f),
        size = Size(w * 0.12f, h * 0.15f)
    )
    // Peaks
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.15f, h * 0.28f),
        size = Size(w * 0.08f, h * 0.09f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.77f, h * 0.28f),
        size = Size(w * 0.08f, h * 0.09f)
    )
}

private fun DrawScope.drawTricepsIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted triceps (back of upper arm)
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.12f, h * 0.27f),
        size = Size(w * 0.13f, h * 0.16f)
    )
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.75f, h * 0.27f),
        size = Size(w * 0.13f, h * 0.16f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.14f, h * 0.30f),
        size = Size(w * 0.09f, h * 0.10f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.77f, h * 0.30f),
        size = Size(w * 0.09f, h * 0.10f)
    )
}

private fun DrawScope.drawLegsIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted quads
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.31f, h * 0.59f),
        size = Size(w * 0.15f, h * 0.21f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.54f, h * 0.59f),
        size = Size(w * 0.15f, h * 0.21f),
        cornerRadius = CornerRadius(w * 0.06f)
    )
    // Highlighted calves
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.33f, h * 0.80f),
        size = Size(w * 0.11f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.04f)
    )
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.56f, h * 0.80f),
        size = Size(w * 0.11f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.04f)
    )
    // Detail
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.34f, h * 0.63f),
        size = Size(w * 0.10f, h * 0.12f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.56f, h * 0.63f),
        size = Size(w * 0.10f, h * 0.12f)
    )
}

private fun DrawScope.drawAbsIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Abs: 3 rows x 2 columns of small rounded rects
    val absColor = MuscleHighlight
    val absLight = MuscleHighlightLight
    val colLeft = w * 0.38f
    val colRight = w * 0.52f
    val absW = w * 0.10f
    val absH = h * 0.06f
    val gap = h * 0.015f
    val startY = h * 0.30f

    for (row in 0..2) {
        val y = startY + row * (absH + gap)
        drawRoundRect(
            absColor,
            topLeft = Offset(colLeft, y),
            size = Size(absW, absH),
            cornerRadius = CornerRadius(w * 0.02f)
        )
        drawRoundRect(
            absColor,
            topLeft = Offset(colRight, y),
            size = Size(absW, absH),
            cornerRadius = CornerRadius(w * 0.02f)
        )
        // Inner highlight
        drawRoundRect(
            absLight,
            topLeft = Offset(colLeft + w * 0.02f, y + h * 0.01f),
            size = Size(absW - w * 0.04f, absH - h * 0.02f),
            cornerRadius = CornerRadius(w * 0.01f)
        )
        drawRoundRect(
            absLight,
            topLeft = Offset(colRight + w * 0.02f, y + h * 0.01f),
            size = Size(absW - w * 0.04f, absH - h * 0.02f),
            cornerRadius = CornerRadius(w * 0.01f)
        )
    }
}

private fun DrawScope.drawGlutesIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted glutes
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.30f, h * 0.48f),
        size = Size(w * 0.18f, h * 0.14f)
    )
    drawOval(
        MuscleHighlight,
        topLeft = Offset(w * 0.52f, h * 0.48f),
        size = Size(w * 0.18f, h * 0.14f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.34f, h * 0.51f),
        size = Size(w * 0.10f, h * 0.08f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.56f, h * 0.51f),
        size = Size(w * 0.10f, h * 0.08f)
    )
}

private fun DrawScope.drawForearmsIcon(w: Float, h: Float) {
    drawFullBody(w, h)
    // Highlighted forearms
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.10f, h * 0.41f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
    drawRoundRect(
        MuscleHighlight,
        topLeft = Offset(w * 0.79f, h * 0.41f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.05f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.12f, h * 0.44f),
        size = Size(w * 0.07f, h * 0.12f)
    )
    drawOval(
        MuscleHighlightLight,
        topLeft = Offset(w * 0.81f, h * 0.44f),
        size = Size(w * 0.07f, h * 0.12f)
    )
}

private fun DrawScope.drawCardioIcon(w: Float, h: Float) {
    // Heart shape
    val heartPath = Path().apply {
        moveTo(w * 0.5f, h * 0.82f)
        cubicTo(w * 0.1f, h * 0.55f, w * 0.05f, h * 0.2f, w * 0.5f, h * 0.35f)
        cubicTo(w * 0.95f, h * 0.2f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.82f)
        close()
    }
    drawPath(heartPath, MuscleHighlight)

    // Inner highlight
    val innerPath = Path().apply {
        moveTo(w * 0.45f, h * 0.70f)
        cubicTo(w * 0.18f, h * 0.50f, w * 0.15f, h * 0.28f, w * 0.45f, h * 0.40f)
        cubicTo(w * 0.60f, h * 0.28f, w * 0.55f, h * 0.45f, w * 0.45f, h * 0.70f)
        close()
    }
    drawPath(innerPath, MuscleHighlightLight.copy(alpha = 0.4f))
}

private fun DrawScope.drawOtherIcon(w: Float, h: Float) {
    // Dumbbell icon
    val barColor = BodyGreyDark
    val plateColor = MuscleHighlight
    val plateLight = MuscleHighlightLight

    // Center bar
    drawRoundRect(
        barColor,
        topLeft = Offset(w * 0.25f, h * 0.46f),
        size = Size(w * 0.50f, h * 0.08f),
        cornerRadius = CornerRadius(w * 0.02f)
    )

    // Left plates
    drawRoundRect(
        plateColor,
        topLeft = Offset(w * 0.10f, h * 0.30f),
        size = Size(w * 0.12f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.03f)
    )
    drawRoundRect(
        plateLight,
        topLeft = Offset(w * 0.18f, h * 0.34f),
        size = Size(w * 0.08f, h * 0.32f),
        cornerRadius = CornerRadius(w * 0.02f)
    )

    // Right plates
    drawRoundRect(
        plateColor,
        topLeft = Offset(w * 0.78f, h * 0.30f),
        size = Size(w * 0.12f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.03f)
    )
    drawRoundRect(
        plateLight,
        topLeft = Offset(w * 0.74f, h * 0.34f),
        size = Size(w * 0.08f, h * 0.32f),
        cornerRadius = CornerRadius(w * 0.02f)
    )
}
