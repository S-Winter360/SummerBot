package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.ActionAuditEntry
import com.example.core.interaction.InteractionState
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.network.NetworkState
import com.example.ui.components.CapabilityAuditTicker
import com.example.ui.components.ConversationInputBar
import com.example.ui.components.StateIndicatorBadge
import com.example.ui.components.StateTransitionSelector
import com.example.ui.components.SummerVisualCore
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted

@Composable
fun MainSummerScreen(
    state: SummerState,
    networkState: NetworkState,
    personality: SummerPersonality,
    latestAudit: ActionAuditEntry?,
    recentAssistantSpeech: String?,
    currentInteraction: SummerInteraction? = null,
    onStateSelected: (SummerState) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onMicTrigger: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showPersonalityDialog by remember { mutableStateOf(false) }

    if (showPersonalityDialog) {
        PersonalityDialog(
            personality = personality,
            onDismiss = { showPersonalityDialog = false }
        )
    }

    val demoPrompts = listOf(
        "Hello Summer",
        "What is your name?",
        "What can you do?",
        "What time is it?",
        "test network",
        "test mic",
        "remember I enjoy quiet evenings"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CoreBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .testTag("main_summer_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Network Isolation & Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoreCharcoalSurface)
                        .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (networkState == NetworkState.OFFLINE) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = "Network condition",
                        tint = if (networkState == NetworkState.OFFLINE) CyanLuminous else SlateLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (networkState == NetworkState.OFFLINE) "OFFLINE MODE" else networkState.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateLight
                    )
                }

                // Action buttons: Personality Info & Settings
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showPersonalityDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoreCharcoalSurface)
                            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .testTag("personality_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Personality details",
                            tint = SlateLight
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoreCharcoalSurface)
                            .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .testTag("settings_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings entry point",
                            tint = CyanLuminous
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Central Visual AI Core / Orb
            SummerVisualCore(
                state = state,
                size = 220.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showPersonalityDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Assistant Name "SUMMER"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = personality.shortName.uppercase(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    color = CyanBright
                )

                Text(
                    text = personality.role.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = SlateLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Current State Indicator
            StateIndicatorBadge(state = state)

            // Interaction Lifecycle Pill (if active)
            if (currentInteraction != null && currentInteraction.processingState != InteractionState.COMPLETED) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoreCharcoalElevated)
                        .border(1.dp, CyanLuminous.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "STAGE: ${currentInteraction.processingState.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanLuminous
                    )
                }
            }

            // Dynamic Context / Speech Bubble
            AnimatedVisibility(
                visible = !recentAssistantSpeech.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (recentAssistantSpeech != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CoreCharcoalSurface.copy(alpha = 0.9f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanLuminous.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = recentAssistantSpeech,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick demo intent chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                demoPrompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CoreCharcoalElevated)
                            .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onSubmitQuery(prompt)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // State Transition Demonstration Switcher
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "STATE TRANSITION ARCHITECTURE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted
                )

                StateTransitionSelector(
                    currentState = state,
                    onStateSelect = onStateSelected
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capability / Security Audit Ticker
            CapabilityAuditTicker(
                latestAudit = latestAudit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Conversation & Microphone Entry Points
            ConversationInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSubmit = {
                    if (inputText.isNotBlank()) {
                        val query = inputText
                        inputText = ""
                        onSubmitQuery(query)
                    }
                },
                onMicTrigger = onMicTrigger,
                isListening = state is SummerState.Listening,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}
