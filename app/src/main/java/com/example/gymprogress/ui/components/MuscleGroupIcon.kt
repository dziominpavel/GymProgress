package com.example.gymprogress.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymprogress.R
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
    val drawableRes = when (muscleGroup) {
        "CHEST" -> R.drawable.ic_muscle_chest
        "BACK" -> R.drawable.ic_muscle_back
        "SHOULDERS" -> R.drawable.ic_muscle_shoulders
        "BICEPS" -> R.drawable.ic_muscle_biceps
        "TRICEPS" -> R.drawable.ic_muscle_triceps
        "LEGS" -> R.drawable.ic_muscle_legs
        "ABS" -> R.drawable.ic_muscle_abs
        else -> null
    }

    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = muscleGroup,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
        )
    } else {
        Canvas(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
        ) {
            val w = this.size.width
            val h = this.size.height
            drawOtherIcon(w, h)
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

// ─── Canvas fallback icons for muscle groups without VectorDrawable ───

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
