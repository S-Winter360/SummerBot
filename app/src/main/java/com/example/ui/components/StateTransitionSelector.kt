package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted

@Composable
fun StateTransitionSelector(
    currentState: SummerState,
    onStateSelect: (SummerState) -> Unit,
    modifier: Modifier = Modifier
) {
    val states = listOf(
        SummerState.Idle,
        SummerState.Listening("Direct speech input"),
        SummerState.Thinking("Evaluating local reasoning graph"),
        SummerState.Speaking("Voice output active"),
        SummerState.Executing("Contextual task sequence"),
        SummerState.Observing("Ambient context"),
        SummerState.Learning("Memory consolidation")
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        states.forEach { targetState ->
            val isSelected = targetState::class == currentState::class
            val shape = RoundedCornerShape(16.dp)

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(shape)
                    .background(if (isSelected) CoreCharcoalElevated else CoreCharcoalSurface.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CyanLuminous.copy(alpha = 0.8f) else CoreCharcoalBorder.copy(alpha = 0.4f),
                        shape = shape
                    )
                    .clickable { onStateSelect(targetState) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("state_pill_${targetState.displayName.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = targetState.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) CyanLuminous else SlateLight
                )
            }
        }
    }
}
