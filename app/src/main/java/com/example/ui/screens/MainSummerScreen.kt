package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.actions.ActionAuditEntry
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.network.NetworkState
import com.example.ui.components.SummerCoreOrb
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreen

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CoreBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("main_summer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = personality.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Offline Cognitive Core",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanBright
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isOnline = networkState != NetworkState.OFFLINE
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CoreCharcoalSurface)
                        .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Network connectivity state",
                            tint = if (isOnline) SuccessGreen else SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) SlateBright else SlateMuted
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoreCharcoalSurface)
                        .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(12.dp))
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = SlateBright
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Central Orb
        SummerCoreOrb(state = state)

        Spacer(modifier = Modifier.height(12.dp))

        // State indicator label
        Text(
            text = state::class.java.simpleName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CyanLuminous,
            modifier = Modifier.testTag("state_label")
        )

        Text(
            text = state.description,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateLight,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Assistant Speech / Response Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("assistant_speech_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CoreCharcoalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUMMER REASONING",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanLuminous
                    )
                    if (currentInteraction != null) {
                        Text(
                            text = currentInteraction.state.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }

                Text(
                    text = recentAssistantSpeech ?: "Awaiting user input...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateBright
                )
            }
        }

        // Quick demo intents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChip(label = "Greeting", onClick = { onSubmitQuery("Hello Summer") }, modifier = Modifier.weight(1f))
            QuickChip(label = "Identity", onClick = { onSubmitQuery("What is your name?") }, modifier = Modifier.weight(1f))
            QuickChip(label = "Capabilities", onClick = { onSubmitQuery("What can you do?") }, modifier = Modifier.weight(1f))
            QuickChip(label = "Time", onClick = { onSubmitQuery("What time is it?") }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("user_input_field"),
                placeholder = {
                    Text("Speak or type to Summer...", color = SlateMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanLuminous,
                    unfocusedBorderColor = CoreCharcoalBorder,
                    focusedContainerColor = CoreCharcoalElevated,
                    unfocusedContainerColor = CoreCharcoalSurface,
                    focusedTextColor = SlateBright,
                    unfocusedTextColor = SlateBright
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            onSubmitQuery(textInput)
                            textInput = ""
                        }
                    }
                )
            )

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSubmitQuery(textInput)
                        textInput = ""
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
                    contentDescription = "Send prompt",
                    tint = CoreBlack
                )
            }

            IconButton(
                onClick = onMicTrigger,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CoreCharcoalElevated)
                    .border(1.dp, CoreCharcoalBorder, CircleShape)
                    .testTag("mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice prompt input",
                    tint = CyanLuminous
                )
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CoreCharcoalSurface)
            .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateLight
        )
    }
}
