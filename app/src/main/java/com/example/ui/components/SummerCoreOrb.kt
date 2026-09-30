package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SummerCoreOrb(
    state: SummerState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (coreColor, haloColor) = when (state) {
        is SummerState.Idle -> Pair(CyanLuminous, CyanMuted)
        is SummerState.Listening -> Pair(CyanBright, CyanLuminous)
        is SummerState.Thinking -> Pair(CyanBright, WarningAmber)
        is SummerState.Speaking -> Pair(CyanLuminous, CyanBright)
        is SummerState.Executing -> Pair(SuccessGreen, CyanMuted)
        is SummerState.Learning -> Pair(CyanBright, SuccessGreen)
        is SummerState.Error -> Pair(ErrorRed, WarningAmber)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("summer_core_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f

            // Outer ethereal glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloColor.copy(alpha = haloAlpha * 0.4f),
                        haloColor.copy(alpha = haloAlpha * 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f * pulseScale
                ),
                radius = baseRadius * 1.35f * pulseScale,
                center = center
            )

            // Dynamic orbital energy ring
            drawCircle(
                color = coreColor.copy(alpha = 0.35f),
                radius = baseRadius * pulseScale,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner dense luminous core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreColor,
                        coreColor.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.65f * pulseScale
                ),
                radius = baseRadius * 0.65f * pulseScale,
                center = center
            )
        }
    }
}
