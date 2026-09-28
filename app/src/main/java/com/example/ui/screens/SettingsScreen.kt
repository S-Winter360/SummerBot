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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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

@Composable
fun SettingsScreen(
    settings: SummerSettings,
    onUpdateSettings: (SummerSettings) -> Unit,
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoreCharcoalSurface)
                    .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(12.dp))
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
