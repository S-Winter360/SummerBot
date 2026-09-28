package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.StateErrorRose
import com.example.ui.theme.StateExecutingIndigo
import com.example.ui.theme.StateIdleCyan
import com.example.ui.theme.StateLearningPurple
import com.example.ui.theme.StateListeningTeal
import com.example.ui.theme.StateObservingEmerald
import com.example.ui.theme.StateSpeakingAmber
import com.example.ui.theme.StateThinkingBlue
import kotlin.math.cos
import kotlin.math.sin

/**
 * The central visual AI core and presence of Summer.
 * Renders an animated luminous quantum core with state-responsive orbital geometry,
 * harmonic breathing, and ethereal energy gradients.
 */
@Composable
fun SummerVisualCore(
    state: SummerState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    // Determine target primary accent color based on active state
    val targetPrimaryColor = when (state) {
        is SummerState.Idle -> StateIdleCyan
        is SummerState.Listening -> StateListeningTeal
        is SummerState.Thinking -> StateThinkingBlue
        is SummerState.Speaking -> StateSpeakingAmber
        is SummerState.Executing -> StateExecutingIndigo
        is SummerState.Observing -> StateObservingEmerald
        is SummerState.Learning -> StateLearningPurple
        is SummerState.Error -> StateErrorRose
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "core_accent_color"
    )

    // Infinite animation drivers for ambient life
    val infiniteTransition = rememberInfiniteTransition(label = "summer_core_life")

    // Subtle breathing pulse for idle / ambient presence
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_breathing"
    )

    // Outer orbital ring rotation
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state is SummerState.Thinking) 4000 else 18000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_outer"
    )

    // Inner orbital ring counter-rotation
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state is SummerState.Thinking) 3000 else 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_inner"
    )

    // Speaking / listening acoustic waveform ripple effect
    val harmonicWave by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    is SummerState.Listening -> 1000
                    is SummerState.Speaking -> 800
                    is SummerState.Executing -> 1400
                    else -> 2600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "harmonic_wave"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("summer_visual_core"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f

            // Dynamic scale factor according to state
            val currentScale = when (state) {
                is SummerState.Idle -> pulseScale
                is SummerState.Listening -> harmonicWave
                is SummerState.Thinking -> 0.98f + (pulseScale - 1f) * 0.5f
                is SummerState.Speaking -> harmonicWave * 0.95f
                is SummerState.Executing -> 0.92f // Focused compression
                else -> pulseScale
            }

            val effectiveRadius = baseRadius * currentScale

            // 1. Outermost soft diffuse ambient aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.32f),
                        animatedAccentColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = effectiveRadius * 1.55f
                ),
                radius = effectiveRadius * 1.55f,
                center = centerOffset
            )

            // 2. Harmonic waveform ring (enhanced in Listening / Speaking)
            if (state is SummerState.Listening || state is SummerState.Speaking) {
                val waveAlpha = if (state is SummerState.Listening) 0.55f else 0.45f
                drawCircle(
                    color = animatedAccentColor.copy(alpha = waveAlpha),
                    radius = effectiveRadius * 1.25f * (harmonicWave / 1.05f),
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Outer delicate geometric orbital ring with broken arcs
            rotate(rotationOuter, pivot = centerOffset) {
                val outerRingRadius = effectiveRadius * 1.18f
                val strokeWidth = 1.5.dp.toPx()

                // Primary segmented arcs
                drawArc(
                    color = animatedAccentColor.copy(alpha = 0.75f),
                    startAngle = 15f,
                    sweepAngle = 75f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - outerRingRadius, centerOffset.y - outerRingRadius),
                    size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = animatedAccentColor.copy(alpha = 0.75f),
                    startAngle = 135f,
                    sweepAngle = 65f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - outerRingRadius, centerOffset.y - outerRingRadius),
                    size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = animatedAccentColor.copy(alpha = 0.45f),
                    startAngle = 240f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - outerRingRadius, centerOffset.y - outerRingRadius),
                    size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
                    style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
                )

                // Delicate orbital tick nodes
                val nodeAngle1 = Math.toRadians(15.0)
                val nodeX1 = centerOffset.x + (outerRingRadius * cos(nodeAngle1)).toFloat()
                val nodeY1 = centerOffset.y + (outerRingRadius * sin(nodeAngle1)).toFloat()
                drawCircle(
                    color = CyanBright,
                    radius = 2.5.dp.toPx(),
                    center = Offset(nodeX1, nodeY1)
                )

                val nodeAngle2 = Math.toRadians(195.0)
                val nodeX2 = centerOffset.x + (outerRingRadius * cos(nodeAngle2)).toFloat()
                val nodeY2 = centerOffset.y + (outerRingRadius * sin(nodeAngle2)).toFloat()
                drawCircle(
                    color = animatedAccentColor,
                    radius = 2.dp.toPx(),
                    center = Offset(nodeX2, nodeY2)
                )
            }

            // 4. Inner counter-rotating precision ring
            rotate(rotationInner, pivot = centerOffset) {
                val innerRingRadius = effectiveRadius * 0.92f
                val strokeWidth = 1.2.dp.toPx()

                drawArc(
                    color = animatedAccentColor.copy(alpha = 0.65f),
                    startAngle = 40f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - innerRingRadius, centerOffset.y - innerRingRadius),
                    size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = animatedAccentColor.copy(alpha = 0.50f),
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - innerRingRadius, centerOffset.y - innerRingRadius),
                    size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 5. Dense core luminous body with depth gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanBright.copy(alpha = 0.95f),
                        animatedAccentColor.copy(alpha = 0.85f),
                        animatedAccentColor.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = effectiveRadius * 0.70f
                ),
                radius = effectiveRadius * 0.70f,
                center = centerOffset
            )

            // 6. Central quantum singularity / bright heart
            val coreHeartRadius = effectiveRadius * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        CyanBright.copy(alpha = 0.90f),
                        animatedAccentColor.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = coreHeartRadius * 1.2f
                ),
                radius = coreHeartRadius,
                center = centerOffset
            )
        }
    }
}
