package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.state.SummerState

/**
 * Organic visual presence core for Summer.
 * Delegates to the unified [SummerCoreOrb] living cognitive core.
 */
@Composable
fun SummerVisualCore(
    state: SummerState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    SummerCoreOrb(
        state = state,
        modifier = modifier.testTag("summer_visual_core"),
        size = size
    )
}
