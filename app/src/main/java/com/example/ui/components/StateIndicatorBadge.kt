package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.StateErrorRose
import com.example.ui.theme.StateExecutingIndigo
import com.example.ui.theme.StateIdleCyan
import com.example.ui.theme.StateLearningPurple
import com.example.ui.theme.StateListeningTeal
import com.example.ui.theme.StateObservingEmerald
import com.example.ui.theme.StateSpeakingAmber
import com.example.ui.theme.StateThinkingBlue

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
        animationSpec = tween(durationMillis = 400),
        label = "state_badge_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "indicator_pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Row(
        modifier = modifier
            .testTag("state_indicator_badge")
            .clip(RoundedCornerShape(20.dp))
            .background(CoreCharcoalSurface.copy(alpha = 0.85f))
            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Glowing status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(dotPulse)
                .clip(CircleShape)
                .background(animatedColor)
        )

        Text(
            text = state.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = animatedColor
        )
    }
}
