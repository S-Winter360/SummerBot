package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted

/**
 * Organic, unified floating interaction surface for communicating with Summer.
 * Combines seamless conversational text entry with ambient microphone presence
 * and glowing floating action feedback.
 */
@Composable
fun ConversationInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMicTrigger: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(32.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "mic_breathing")
    val micGlowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.50f else 0.15f,
        targetValue = if (isListening) 0.90f else 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_glow_alpha"
    )

    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isListening) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(CoreCharcoalSurface.copy(alpha = 0.85f))
            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.55f), barShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Integrated Ambient Microphone Touch Control
        IconButton(
            onClick = onMicTrigger,
            modifier = Modifier
                .size(48.dp)
                .scale(micPulseScale)
                .clip(CircleShape)
                .background(CyanLuminous.copy(alpha = micGlowAlpha))
                .border(
                    width = 1.dp,
                    color = CyanBright.copy(alpha = if (isListening) 0.8f else 0.3f),
                    shape = CircleShape
                )
                .testTag("mic_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone voice trigger",
                tint = if (isListening) CyanBright else SlateBright,
                modifier = Modifier.size(22.dp)
            )
        }

        // Conversational text field
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Speak or type to Summer...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateMuted
                )
            }

            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = SlateBright
                ),
                cursorBrush = SolidColor(CyanLuminous),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSubmit()
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_input_field")
                    .testTag("conversation_text_field")
            )
        }

        // Glowing Floating Send Button
        AnimatedVisibility(
            visible = inputText.isNotBlank(),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSubmit()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CyanLuminous)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = CoreBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
