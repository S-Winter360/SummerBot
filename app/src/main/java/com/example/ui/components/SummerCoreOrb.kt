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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState
import com.example.ui.theme.CyanBright
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
 * Summer's Living AI Core.
 *
 * Designed with Organic Futurism:
 * - A living cognitive core with soft breathing illumination
 * - Multi-layered organic radial fields and gently morphing contours
 * - Subtle bioluminescent particle drift and fluid energy movement
 * - Completely avoids mechanical HUDs, concentric technical rings, or radar grids
 */
@Composable
fun SummerCoreOrb(
    state: SummerState,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val (primaryColor, ambientColor) = when (state) {
        is SummerState.Idle -> Pair(StateIdleCyan, CyanMuted)
        is SummerState.Listening -> Pair(StateListeningTeal, CyanLuminous)
        is SummerState.Thinking -> Pair(StateThinkingBlue, StateSpeakingAmber)
        is SummerState.Speaking -> Pair(CyanBright, StateSpeakingAmber)
        is SummerState.Executing -> Pair(StateExecutingIndigo, StateIdleCyan)
        is SummerState.Observing -> Pair(StateObservingEmerald, StateListeningTeal)
        is SummerState.Learning -> Pair(StateLearningPurple, CyanLuminous)
        is SummerState.Error -> Pair(StateErrorRose, CyanMuted)
    }

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "core_primary_color"
    )

    val animatedAmbient by animateColorAsState(
        targetValue = ambientColor,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "core_ambient_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "living_core_presence")

    // Slow, organic respiratory breath cycle (approx 3.8s period)
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    is SummerState.Listening -> 2200
                    is SummerState.Speaking -> 1800
                    is SummerState.Executing -> 2600
                    else -> 3800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // Ethereal contour morphing phase (fluid organic deformation)
    val contourPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state is SummerState.Thinking) 6000 else 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "contour_phase"
    )

    // Light focal drift (subtle wander of internal illumination center)
    val focalWanderX by infiniteTransition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focal_wander_x"
    )

    val focalWanderY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = -0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focal_wander_y"
    )

    // Outward energy ripple for Listening / Speaking
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state is SummerState.Speaking) 1600 else 2400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    // Bioluminescent mote drift phase
    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_drift"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("summer_core_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.70f
            val currentRadius = baseRadius * breathScale

            // Fluid drifting focal center for organic illumination
            val focalOffset = Offset(
                x = center.x + (currentRadius * focalWanderX),
                y = center.y + (currentRadius * focalWanderY)
            )

            // 1. Outermost Ambient Atmospheric Glow (soft diffuse breathing field)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.22f),
                        animatedAmbient.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = focalOffset,
                    radius = currentRadius * 1.55f
                ),
                radius = currentRadius * 1.55f,
                center = center
            )

            // 2. State-Specific Living Dynamics
            when (state) {
                is SummerState.Listening -> {
                    // Gentle outward propagating energy ripples
                    val rippleRadius = currentRadius * (0.95f + rippleProgress * 0.45f)
                    val rippleAlpha = (1f - rippleProgress) * 0.35f
                    drawCircle(
                        color = animatedPrimary.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                is SummerState.Speaking -> {
                    // Responsive fluid acoustic undulation
                    val waveAlpha = (1f - rippleProgress) * 0.38f
                    val waveRadius1 = currentRadius * (0.92f + rippleProgress * 0.38f)
                    drawCircle(
                        color = animatedPrimary.copy(alpha = waveAlpha),
                        radius = waveRadius1,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Secondary offset acoustic wave
                    val secondaryProgress = (rippleProgress + 0.5f) % 1f
                    val secondaryRadius = currentRadius * (0.92f + secondaryProgress * 0.38f)
                    val secondaryAlpha = (1f - secondaryProgress) * 0.25f
                    drawCircle(
                        color = animatedAmbient.copy(alpha = secondaryAlpha),
                        radius = secondaryRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                is SummerState.Thinking -> {
                    // Internal swirling secondary thought focus
                    val thinkAngle = contourPhase * 1.5f
                    val thinkOffset = Offset(
                        x = center.x + (currentRadius * 0.28f * cos(thinkAngle)),
                        y = center.y + (currentRadius * 0.28f * sin(thinkAngle))
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StateSpeakingAmber.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = thinkOffset,
                            radius = currentRadius * 0.65f
                        ),
                        radius = currentRadius * 0.65f,
                        center = thinkOffset
                    )
                }

                is SummerState.Observing -> {
                    // Soft horizontal ambient sweeping field
                    val sweepOffset = Offset(
                        x = center.x + (currentRadius * 0.25f * sin(contourPhase)),
                        y = center.y
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StateObservingEmerald.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = sweepOffset,
                            radius = currentRadius * 0.9f
                        ),
                        radius = currentRadius * 0.9f,
                        center = sweepOffset
                    )
                }

                is SummerState.Learning -> {
                    // Layered blooming soft aura
                    val bloomRadius = currentRadius * (1.05f + 0.15f * sin(contourPhase))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                StateLearningPurple.copy(alpha = 0.28f),
                                animatedPrimary.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = bloomRadius
                        ),
                        radius = bloomRadius,
                        center = center
                    )
                }

                else -> {
                    // Idle & Execution subtle ambient glow
                }
            }

            // 3. Organic Fluid Membrane (Subtle Bezier Contour)
            // 8 smoothly connected organic harmonic points
            val numPoints = 8
            val path = Path()
            val points = ArrayList<Offset>(numPoints)

            for (i in 0 until numPoints) {
                val angle = (i.toFloat() / numPoints) * 2 * Math.PI.toFloat()
                // Perturbation based on harmonic wave and state
                val waveFactor = when (state) {
                    is SummerState.Speaking -> 0.055f * sin(3 * angle + contourPhase * 2f)
                    is SummerState.Thinking -> 0.045f * cos(2 * angle - contourPhase)
                    is SummerState.Listening -> 0.035f * sin(4 * angle + contourPhase)
                    else -> 0.025f * sin(3 * angle + contourPhase)
                }
                val r = currentRadius * (0.96f + waveFactor)
                val px = center.x + r * cos(angle)
                val py = center.y + r * sin(angle)
                points.add(Offset(px, py))
            }

            // Construct smooth closed spline through points
            if (points.isNotEmpty()) {
                path.moveTo((points[0].x + points[numPoints - 1].x) / 2f, (points[0].y + points[numPoints - 1].y) / 2f)
                for (i in 0 until numPoints) {
                    val next = points[(i + 1) % numPoints]
                    val curr = points[i]
                    val midX = (curr.x + next.x) / 2f
                    val midY = (curr.y + next.y) / 2f
                    path.quadraticTo(curr.x, curr.y, midX, midY)
                }
                path.close()

                // Draw translucent organic membrane body
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.55f),
                            animatedAmbient.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = focalOffset,
                        radius = currentRadius * 0.95f
                    )
                )

                // Soft luminous perimeter stroke (gentle non-mechanical rim)
                drawPath(
                    path = path,
                    color = animatedPrimary.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Inner Luminous Core (Soft Ethereal Singularity)
            val heartRadius = currentRadius * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        CyanBright.copy(alpha = 0.85f),
                        animatedPrimary.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = focalOffset,
                    radius = heartRadius * 1.15f
                ),
                radius = heartRadius,
                center = center
            )

            // 5. Bioluminescent Spores / Ambient Light Motes
            // 6 microscopic drifting particles that softly breathe in the energy field
            val particleCount = 6
            for (p in 0 until particleCount) {
                val baseAngle = (p.toFloat() / particleCount) * 2 * Math.PI.toFloat()
                val particleAngle = baseAngle + particleDrift * (if (p % 2 == 0) 0.6f else -0.4f)
                val distFactor = 0.75f + 0.32f * sin(particleDrift * 1.5f + p)
                val px = center.x + currentRadius * distFactor * cos(particleAngle)
                val py = center.y + currentRadius * distFactor * sin(particleAngle)
                val pAlpha = 0.30f + 0.35f * sin(particleDrift * 2f + p)

                drawCircle(
                    color = animatedPrimary.copy(alpha = pAlpha.coerceIn(0.1f, 0.7f)),
                    radius = (1.5f + (p % 2)).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
