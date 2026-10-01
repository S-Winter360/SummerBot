package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.state.SummerState
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.SlateLight
import com.example.ui.theme.StateErrorRose
import com.example.ui.theme.StateExecutingIndigo
import com.example.ui.theme.StateIdleCyan
import com.example.ui.theme.StateLearningPurple
import com.example.ui.theme.StateListeningTeal
import com.example.ui.theme.StateObservingEmerald
import com.example.ui.theme.StateSpeakingAmber
import com.example.ui.theme.StateThinkingBlue

fun getCompanionStatusPhrase(state: SummerState): String = when (state) {
    is SummerState.Idle -> "Ready when you are."
    is SummerState.Listening -> "I'm listening."
    is SummerState.Thinking -> "Thinking..."
    is SummerState.Speaking -> "Speaking..."
    is SummerState.Executing -> "Working on it."
    is SummerState.Observing -> "Observing."
    is SummerState.Learning -> "Learning."
    is SummerState.Error -> "Something went wrong."
}

@Composable
fun StateIndicatorBadge(
    state: SummerState,
    modifier: Modifier = Modifier
) {
    val stateColor = when (state) {
        is SummerState.Idle -> StateIdleCyan
        is SummerState.Listening -> StateListeningTeal
        is SummerState.Thinking -> StateThinkingBlue
        is SummerState.Speaking -> StateSpeakingAmber
        is SummerState.Executing -> StateExecutingIndigo
        is SummerState.Observing -> StateObservingEmerald
        is SummerState.Learning -> StateLearningPurple
        is SummerState.Error -> StateErrorRose
    }

    val animatedColor by animateColorAsState(
        targetValue = stateColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "state_badge_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "indicator_pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Soft floating organic state pill
        Row(
            modifier = Modifier
                .testTag("state_indicator_badge")
                .clip(RoundedCornerShape(24.dp))
                .background(CoreCharcoalSurface.copy(alpha = 0.65f))
                .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gentle bioluminescent status dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(dotPulse)
                    .clip(CircleShape)
                    .background(animatedColor)
            )

            Text(
                text = state.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = animatedColor,
                modifier = Modifier.testTag("state_label")
            )
        }

        // Natural companion status phrase
        Text(
            text = getCompanionStatusPhrase(state),
            style = MaterialTheme.typography.bodyMedium,
            color = SlateLight
        )
    }
}
