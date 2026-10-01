package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.ActionAuditEntry
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.network.NetworkState
import com.example.ui.components.CapabilityAuditTicker
import com.example.ui.components.ConversationInputBar
import com.example.ui.components.StateIndicatorBadge
import com.example.ui.components.SummerCoreOrb
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreen

/**
 * Summer's Home Screen — Organic Futurism.
 *
 * Designed around a living, calm AI companion:
 * - Minimal, quiet dark space illuminated by a breathing cyan/teal cognitive core
 * - Lighter, understated companion header with subtle live connectivity dot
 * - Floating thought surface for natural dialogue instead of heavy diagnostic cards
 * - Conversational quick suggestions
 * - Unified organic interaction surface for voice and text
 */
@Composable
fun MainSummerScreen(
    state: SummerState,
    networkState: NetworkState,
    personality: SummerPersonality,
    latestAudit: ActionAuditEntry?,
    recentAssistantSpeech: String?,
    currentInteraction: SummerInteraction?,
    onStateSelected: (SummerState) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onMicTrigger: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val isOnline = networkState != NetworkState.OFFLINE

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_presence")
    val networkDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "network_dot_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CoreBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("main_summer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Companion Header (Lighter, dignified, companion-oriented)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = personality.shortName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.8.sp
                    ),
                    color = SlateBright
                )

                // Understated, organic live connectivity indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(networkDotPulse)
                            .clip(CircleShape)
                            .background(if (isOnline) SuccessGreen else SlateMuted)
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOnline) SlateLight else SlateMuted
                    )
                }
            }

            // Soft floating circular settings button
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CoreCharcoalSurface.copy(alpha = 0.65f))
                    .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.40f), CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = SlateLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Central Living AI Core (Dominant breathing presence)
        SummerCoreOrb(
            state = state,
            size = 250.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. State & Companion Presence Phrase
        StateIndicatorBadge(
            state = state,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Floating Thought / Conversation Surface
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("assistant_speech_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CoreCharcoalSurface.copy(alpha = 0.65f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = personality.shortName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = CyanLuminous
                    )

                    if (currentInteraction != null) {
                        Text(
                            text = currentInteraction.state.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }

                Text(
                    text = recentAssistantSpeech ?: "Hello. I'm here when you need me.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateBright,
                    lineHeight = 22.sp
                )
            }
        }

        // 5. Capability Audit Ticker (Understated security feedback)
        CapabilityAuditTicker(
            latestAudit = latestAudit,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Conversational Suggestions (Natural, floating chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrganicSuggestionChip(
                label = "Say hello",
                onClick = { onSubmitQuery("Hello Summer") },
                modifier = Modifier.weight(1f)
            )
            OrganicSuggestionChip(
                label = "Who are you?",
                onClick = { onSubmitQuery("What is your name?") },
                modifier = Modifier.weight(1f)
            )
            OrganicSuggestionChip(
                label = "Capabilities",
                onClick = { onSubmitQuery("What can you do?") },
                modifier = Modifier.weight(1f)
            )
            OrganicSuggestionChip(
                label = "Time",
                onClick = { onSubmitQuery("What time is it?") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Unified Organic Floating Interaction Surface
        ConversationInputBar(
            inputText = textInput,
            onInputChange = { textInput = it },
            onSubmit = {
                if (textInput.isNotBlank()) {
                    onSubmitQuery(textInput)
                    textInput = ""
                }
            },
            onMicTrigger = onMicTrigger,
            isListening = state is SummerState.Listening,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )
    }
}

/**
 * Organic, soft floating suggestion chip.
 */
@Composable
private fun OrganicSuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CoreCharcoalSurface.copy(alpha = 0.50f))
            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SlateLight
        )
    }
}
