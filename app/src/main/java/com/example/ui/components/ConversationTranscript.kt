package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.ConversationMessage
import com.example.ui.models.ConversationSpeaker
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted

/**
 * Organic Futurism Conversation Transcript.
 *
 * Renders in-session conversation turns between User and Summer with timestamps,
 * speaker badges, gentle speech bubble styling, and a developer "Copy Transcript" action.
 */
@Composable
fun ConversationTranscript(
    messages: List<ConversationMessage>,
    modifier: Modifier = Modifier,
    onCopyTranscript: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Auto-scroll to latest message when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val handleCopy = {
        if (onCopyTranscript != null) {
            onCopyTranscript()
        } else {
            val textToCopy = formatTranscriptForClipboard(messages)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Summer Conversation Transcript", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversation_transcript_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.70f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.50f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Transcript title and Copy action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CoreCharcoalElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "CONVERSATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = CyanLuminous
                    )

                    Text(
                        text = "(${messages.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }

                OutlinedButton(
                    onClick = handleCopy,
                    modifier = Modifier.testTag("copy_transcript_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanBright
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        CyanLuminous.copy(alpha = 0.40f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy conversation transcript",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Message Turns List
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet in this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ConversationMessageBubble(message = message)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.speaker == ConversationSpeaker.USER

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcript_message_${message.speaker.name.lowercase()}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Speaker metadata row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (!isUser) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = CyanLuminous,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = SlateLight,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = if (isUser) "You" else "Summer",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isUser) SlateLight else CyanBright
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted
            )

            Text(
                text = message.formatTime(),
                style = MaterialTheme.typography.labelSmall,
                color = SlateMuted,
                fontSize = 10.sp
            )
        }

        // Message text bubble
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) CoreCharcoalElevated.copy(alpha = 0.90f)
                    else CoreCharcoalSurface.copy(alpha = 0.95f)
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) CoreCharcoalBorder.copy(alpha = 0.60f)
                    else CyanLuminous.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBright,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Formats full conversation transcript for copying to clipboard or diagnostics report.
 */
fun formatTranscriptForClipboard(messages: List<ConversationMessage>): String {
    val sb = StringBuilder()
    sb.appendLine("=== SUMMER CONVERSATION TRANSCRIPT ===")
    sb.appendLine("Generated: ${java.util.Date()}")
    sb.appendLine("Total turns: ${messages.size}")
    sb.appendLine("======================================")
    sb.appendLine()
    for (msg in messages) {
        val speakerName = if (msg.speaker == ConversationSpeaker.USER) "User" else "Summer"
        sb.appendLine("[${msg.formatTime()}] $speakerName (${msg.source.name}):")
        sb.appendLine(msg.text)
        sb.appendLine()
    }
    return sb.toString().trimEnd()
}
