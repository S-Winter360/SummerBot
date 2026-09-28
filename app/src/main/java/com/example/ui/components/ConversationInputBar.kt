package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted

@Composable
fun ConversationInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMicTrigger: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(28.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(CoreCharcoalSurface.copy(alpha = 0.90f))
            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.8f), barShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Voice / Microphone Trigger
        IconButton(
            onClick = onMicTrigger,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isListening) CyanLuminous.copy(alpha = 0.2f) else CoreCharcoalElevated)
                .testTag("mic_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone voice trigger",
                tint = if (isListening) CyanLuminous else SlateLight
            )
        }

        // Text query entry box
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Message Summer or test capabilities…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
            }

            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(CyanLuminous),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversation_text_field")
            )
        }

        // Send Button
        IconButton(
            onClick = onSubmit,
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .testTag("send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (inputText.isNotBlank()) CyanLuminous else SlateMuted
            )
        }
    }
}
