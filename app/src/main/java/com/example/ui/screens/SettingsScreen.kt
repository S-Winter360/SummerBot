package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.capability.AIDiagnostics
import com.example.ai.capability.AIAvailabilityStatus
import com.example.ai.capability.AIProviderType
import com.example.memory.models.SummerSettings
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: SummerSettings,
    aiDiagnostics: AIDiagnostics = AIDiagnostics(),
    onUpdateSettings: (SummerSettings) -> Unit,
    onRefreshAIDiagnostics: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CoreBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CoreCharcoalSurface.copy(alpha = 0.65f))
                    .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.40f), CircleShape)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to companion",
                    tint = SlateBright
                )
            }

            Column {
                Text(
                    text = "System Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Capabilities & Architectural Boundaries",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }
        }

        // Section: Master Switch
        SettingsToggleCard(
            title = "Summer Enabled",
            subtitle = "Master switch controlling all cognitive modules and action execution",
            icon = Icons.Default.Shield,
            checked = settings.summerEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(summerEnabled = it)) },
            tag = "toggle_summer_enabled"
        )

        // Phase 0C-R1: Local AI & Capability Diagnostics
        Text(
            text = "LOCAL AI & CAPABILITY DIAGNOSTICS",
            style = MaterialTheme.typography.labelSmall,
            color = CyanLuminous,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        AIDiagnosticsCard(
            diagnostics = aiDiagnostics,
            onRefresh = onRefreshAIDiagnostics
        )

        // Section: Voice & Audio Interaction
        Text(
            text = "VOICE & AUDIO FOUNDATION",
            style = MaterialTheme.typography.labelSmall,
            color = CyanLuminous,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        SettingsToggleCard(
            title = "Voice Interaction",
            subtitle = "Enable speech input processing and voice synthesis pipelines",
            icon = Icons.Default.Mic,
            checked = settings.voiceInteractionEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(voiceInteractionEnabled = it)) },
            tag = "toggle_voice_interaction"
        )

        SettingsToggleCard(
            title = "Wake Word",
            subtitle = "Local wake-word detection placeholder (deferred to wake-word phase)",
            icon = Icons.Default.Hearing,
            checked = settings.wakeWordEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(wakeWordEnabled = it)) },
            tag = "toggle_wake_word"
        )

        SettingsToggleCard(
            title = "Speaker Recognition",
            subtitle = "Voiceprint matching and authorized speaker identification",
            icon = Icons.Default.RecordVoiceOver,
            checked = settings.speakerRecognitionEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(speakerRecognitionEnabled = it)) },
            tag = "toggle_speaker_recognition"
        )

        // Section: Autonomy & Interaction
        Text(
            text = "BEHAVIOR & CONTEXT",
            style = MaterialTheme.typography.labelSmall,
            color = CyanLuminous,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        SettingsToggleCard(
            title = "Proactive Responses",
            subtitle = "Allow contextual suggestions based on environmental observations",
            icon = Icons.Default.TouchApp,
            checked = settings.proactiveResponsesEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(proactiveResponsesEnabled = it)) },
            tag = "toggle_proactive_responses"
        )

        SettingsToggleCard(
            title = "Camera Access",
            subtitle = "Allow camera frames to pass to local vision reasoning pipeline",
            icon = Icons.Default.CameraAlt,
            checked = settings.cameraAccessAllowed,
            onCheckedChange = { onUpdateSettings(settings.copy(cameraAccessAllowed = it)) },
            tag = "toggle_camera_access"
        )

        SettingsToggleCard(
            title = "Internet Access",
            subtitle = "Allow network requests when cloud knowledge is explicitly needed",
            icon = Icons.Default.CloudQueue,
            checked = settings.internetAccessAllowed,
            onCheckedChange = { onUpdateSettings(settings.copy(internetAccessAllowed = it)) },
            tag = "toggle_internet_access"
        )

        // Section: Local Memory & Learning
        Text(
            text = "MEMORY & LEARNING",
            style = MaterialTheme.typography.labelSmall,
            color = CyanLuminous,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        SettingsToggleCard(
            title = "Personal Memory",
            subtitle = "Retain user preferences, conversation history, and factual records",
            icon = Icons.Default.Storage,
            checked = settings.personalMemoryEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(personalMemoryEnabled = it)) },
            tag = "toggle_personal_memory"
        )

        SettingsToggleCard(
            title = "Continuous Learning",
            subtitle = "Allow autonomous adaptation to user habits stored locally in Room DB",
            icon = Icons.Default.Psychology,
            checked = settings.learningEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(learningEnabled = it)) },
            tag = "toggle_learning"
        )

        // Architecture diagnostic footer card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CoreCharcoalSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LOCAL PERSISTENCE ARCHITECTURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanBright
                )
                Text(
                    text = "All settings and memories are stored exclusively in an on-device Room SQLite database. No settings are synced to external cloud services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AIDiagnosticsCard(
    diagnostics: AIDiagnostics,
    onRefresh: () -> Unit
) {
    val formattedTime = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(diagnostics.lastCheckedTimestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_diagnostics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoreCharcoalElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Local AI Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateBright
                        )
                        Text(
                            text = "Official Prompt API Status",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }

                // Status Badge
                val (badgeColor, textColor) = when (diagnostics.runtimeStatus) {
                    AIAvailabilityStatus.AVAILABLE -> Pair(CyanLuminous.copy(alpha = 0.2f), CyanBright)
                    AIAvailabilityStatus.DOWNLOADABLE -> Pair(CyanLuminous.copy(alpha = 0.15f), CyanLuminous)
                    AIAvailabilityStatus.DOWNLOADING -> Pair(CyanLuminous.copy(alpha = 0.15f), CyanBright)
                    AIAvailabilityStatus.CHECKING -> Pair(CoreCharcoalElevated, SlateLight)
                    AIAvailabilityStatus.UNAVAILABLE, AIAvailabilityStatus.NOT_SUPPORTED -> Pair(CoreCharcoalElevated, SlateMuted)
                    AIAvailabilityStatus.ERROR -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = diagnostics.runtimeStatus.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Explanatory status description
            val statusExplanation = when (diagnostics.runtimeStatus) {
                AIAvailabilityStatus.AVAILABLE -> "On-device GenAI available"
                AIAvailabilityStatus.DOWNLOADABLE -> "Gemini Nano can be installed on this device."
                AIAvailabilityStatus.DOWNLOADING -> "Gemini Nano is currently downloading."
                AIAvailabilityStatus.UNAVAILABLE -> "On-device GenAI unavailable"
                AIAvailabilityStatus.NOT_SUPPORTED -> "On-device GenAI not supported"
                AIAvailabilityStatus.CHECKING -> "Checking local AI capability..."
                AIAvailabilityStatus.ERROR -> "Capability check error"
            }

            Text(
                text = statusExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = if (diagnostics.runtimeStatus == AIAvailabilityStatus.AVAILABLE) CyanBright else SlateLight,
                fontWeight = FontWeight.Medium
            )

            // Rule 17: Distinguish Detected Provider from Active Provider
            DiagnosticRow(label = "Detected Provider", value = diagnostics.detectedProvider.displayName)
            DiagnosticRow(
                label = "Active Provider",
                value = if (diagnostics.activeProvider == AIProviderType.ON_DEVICE_GENAI) "On-Device GenAI" else "Deterministic Local"
            )
            DiagnosticRow(label = "Active Model", value = diagnostics.currentModel)
            DiagnosticRow(
                label = "Capabilities",
                value = if (diagnostics.supportedCapabilities.isEmpty()) "Not available"
                else diagnostics.supportedCapabilities.joinToString(", ") { it.title }
            )
            DiagnosticRow(
                label = "Fallback Status",
                value = if (diagnostics.isFallbackActive) "Active (Deterministic Core)" else "Standby"
            )
            DiagnosticRow(
                label = "AICore Present",
                value = if (diagnostics.isAiCoreInstalled) "Installed" else "Not detected"
            )
            DiagnosticRow(
                label = "Device Profile",
                value = "Android API ${diagnostics.deviceApiLevel} (${diagnostics.deviceManufacturer} ${diagnostics.deviceModel})"
            )
            DiagnosticRow(
                label = "Network Status",
                value = if (diagnostics.isNetworkAvailable) "Online" else "Offline"
            )
            DiagnosticRow(label = "Last Checked", value = formattedTime)

            if (diagnostics.errorMessage != null) {
                Text(
                    text = "Status notice: ${diagnostics.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("refresh_ai_status_button")
                    .testTag("refresh_diagnostics_button")
                    .testTag("ai_refresh_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CyanLuminous
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanLuminous.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Refresh AI Status",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SlateLight
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = SlateBright,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoreCharcoalElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) CyanLuminous else SlateMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CoreBlack,
                    checkedTrackColor = CyanLuminous,
                    uncheckedThumbColor = SlateLight,
                    uncheckedTrackColor = CoreCharcoalElevated,
                    uncheckedBorderColor = CoreCharcoalBorder
                )
            )
        }
    }
}
