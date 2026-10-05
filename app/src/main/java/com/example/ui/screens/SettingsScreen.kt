package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.ai.localmodel.EmbeddedModelStatus
import com.example.memory.models.SummerSettings
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.voice.models.VoiceProfileId
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
    onImportEmbeddedModel: (Uri) -> Unit = {},
    onCancelEmbeddedModelDownload: () -> Unit = {},
    onDeleteEmbeddedModel: () -> Unit = {},
    onClearMemories: () -> Unit = {},
    onClearConversation: () -> Unit = {},
    onSelectVoiceProfile: (VoiceProfileId) -> Unit = {},
    onSetSpeechSpeed: (Float) -> Unit = {},
    onPreviewVoice: () -> Unit = {},
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

        // Phase 0C-R1 / Phase 0I: Local AI & Capability Diagnostics
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

        // Phase 0I-R1: Embedded Local Model Management via Storage Access Framework
        EmbeddedModelManagementCard(
            diagnostics = aiDiagnostics,
            onImport = onImportEmbeddedModel,
            onCancel = onCancelEmbeddedModelDownload,
            onDelete = onDeleteEmbeddedModel
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

        // Phase 0F: Voice Architecture & Natural Speech Settings Card
        if (settings.voiceInteractionEnabled) {
            VoiceSettingsCard(
                settings = settings,
                diagnostics = aiDiagnostics,
                onSelectVoiceProfile = onSelectVoiceProfile,
                onSetSpeechSpeed = onSetSpeechSpeed,
                onPreviewVoice = onPreviewVoice
            )

            // Phase 0H: Speech Recognition & Foreground Listening Diagnostics
            SpeechInputDiagnosticsCard(diagnostics = aiDiagnostics)
        }

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

        // Phase 0G: Minimal Vision & Camera Architecture Diagnostics
        VisionDiagnosticsCard(diagnostics = aiDiagnostics)

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

        // Local Memory & Context Subsystem Card
        MemoryDiagnosticsCard(
            settings = settings,
            diagnostics = aiDiagnostics,
            onClearMemories = onClearMemories,
            onClearConversation = onClearConversation
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
                        val providerSubtitle = when (diagnostics.activeProvider) {
                            AIProviderType.ON_DEVICE_GENAI -> "On-Device GenAI (Gemini Nano)"
                            AIProviderType.EMBEDDED_LOCAL_MODEL -> "Embedded Local Model (LiteRT-LM)"
                            else -> "Deterministic Offline Core"
                        }
                        Text(
                            text = providerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }

                // Status Badge
                val (badgeColor, textColor) = when (diagnostics.effectiveStatus) {
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
                        text = diagnostics.effectiveStatus.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Explanatory status description
            val statusExplanation = when {
                diagnostics.activeProvider == AIProviderType.EMBEDDED_LOCAL_MODEL ->
                    "Embedded local generative AI active & offline"
                diagnostics.activeProvider == AIProviderType.ON_DEVICE_GENAI ->
                    "On-device GenAI active via Gemini Nano"
                diagnostics.isFallbackActive ->
                    "Deterministic local fallback active"
                else ->
                    "Local AI cognitive core ready"
            }

            Text(
                text = statusExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = if (diagnostics.effectiveStatus == AIAvailabilityStatus.AVAILABLE) CyanBright else SlateLight,
                fontWeight = FontWeight.Medium
            )

            // Distinct provider & model states
            DiagnosticRow(label = "Local AI Engine", value = diagnostics.effectiveStatus.label)
            DiagnosticRow(label = "Active Provider", value = diagnostics.activeProvider.displayName)
            DiagnosticRow(label = "Active Model", value = diagnostics.currentModel)
            DiagnosticRow(label = "Runtime", value = diagnostics.activeRuntime)
            if (diagnostics.activeQuantization != null) {
                DiagnosticRow(label = "Quantization", value = diagnostics.activeQuantization)
            }
            DiagnosticRow(label = "Mode", value = diagnostics.executionMode)
            DiagnosticRow(label = "On-device GenAI", value = diagnostics.onDeviceGenAIStatus.label)
            DiagnosticRow(
                label = "Gemini Nano",
                value = if (diagnostics.onDeviceGenAIStatus == AIAvailabilityStatus.AVAILABLE) "Available"
                else if (diagnostics.isAiCoreInstalled) "Unavailable on this device"
                else "Not available on this device"
            )
            val embeddedLabel = if (diagnostics.embeddedModelStatus == EmbeddedModelStatus.READY || diagnostics.embeddedModelStatus == EmbeddedModelStatus.RUNNING) {
                "Available"
            } else {
                diagnostics.embeddedModelStatus.label
            }
            DiagnosticRow(label = "Embedded Local AI", value = embeddedLabel)
            DiagnosticRow(
                label = "AICore Present",
                value = if (diagnostics.isAiCoreInstalled) "Installed" else "Not detected"
            )
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
private fun EmbeddedModelManagementCard(
    diagnostics: AIDiagnostics,
    onImport: (Uri) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val modelDiag = diagnostics.embeddedModelDiagnostics
    val status = diagnostics.embeddedModelStatus

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImport(uri)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("embedded_model_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Gemma 3 1B IT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateBright
                        )
                        Text(
                            text = if (status == EmbeddedModelStatus.READY) "Installed locally" else "Offline Generative Model",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == EmbeddedModelStatus.READY) CyanBright else SlateMuted
                        )
                    }
                }

                // Status Badge
                val (badgeColor, textColor) = when (status) {
                    EmbeddedModelStatus.READY -> Pair(CyanLuminous.copy(alpha = 0.2f), CyanBright)
                    EmbeddedModelStatus.DOWNLOADING, EmbeddedModelStatus.VERIFYING, EmbeddedModelStatus.INITIALIZING, EmbeddedModelStatus.CHECKING -> Pair(CyanLuminous.copy(alpha = 0.15f), CyanLuminous)
                    EmbeddedModelStatus.NOT_INSTALLED, EmbeddedModelStatus.CANCELLED -> Pair(CoreCharcoalElevated, SlateMuted)
                    EmbeddedModelStatus.INSUFFICIENT_STORAGE, EmbeddedModelStatus.ERROR, EmbeddedModelStatus.CORRUPTED, EmbeddedModelStatus.INCOMPATIBLE_DEVICE -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
                    else -> Pair(CoreCharcoalElevated, SlateLight)
                }

                val badgeText = when (status) {
                    EmbeddedModelStatus.DOWNLOADING, EmbeddedModelStatus.CHECKING, EmbeddedModelStatus.VERIFYING -> "IMPORTING"
                    EmbeddedModelStatus.CORRUPTED -> "MODEL INVALID"
                    else -> status.label.uppercase()
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (status == EmbeddedModelStatus.READY) {
                    "This model runs locally on your device. No internet connection is required for inference."
                } else {
                    "Select a compatible .litertlm model stored on this device."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SlateLight
            )

            DiagnosticRow(label = "Target Artifact", value = "Gemma 3 1B IT (.litertlm)")
            DiagnosticRow(label = "Inference Backend", value = "LiteRT-LM (${modelDiag.activeBackend})")

            if (status == EmbeddedModelStatus.DOWNLOADING || status == EmbeddedModelStatus.VERIFYING || status == EmbeddedModelStatus.CHECKING) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val downloadedMb = modelDiag.downloadedBytes / (1024 * 1024)
                    val totalMb = if (modelDiag.totalBytesToDownload > 0) modelDiag.totalBytesToDownload / (1024 * 1024) else 584L
                    val progressPercent = (modelDiag.downloadProgress * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Importing model file...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanLuminous
                        )
                        Text(
                            text = if (totalMb > 0) "$downloadedMb / $totalMb MB ($progressPercent%)" else "$downloadedMb MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateBright
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (modelDiag.downloadProgress > 0f) modelDiag.downloadProgress else 0.5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanLuminous,
                        trackColor = CoreCharcoalElevated
                    )
                }
            }

            if (status == EmbeddedModelStatus.INSUFFICIENT_STORAGE) {
                Text(
                    text = "Not enough storage is available to install this local AI model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            } else if (status in listOf(EmbeddedModelStatus.ERROR, EmbeddedModelStatus.CORRUPTED)) {
                Text(
                    text = modelDiag.lastError ?: "The selected file is not a valid LiteRT-LM model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (status) {
                    EmbeddedModelStatus.NOT_INSTALLED, EmbeddedModelStatus.CANCELLED -> {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_embedded_model_button")
                                .testTag("install_embedded_model_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanLuminous,
                                contentColor = CoreBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "IMPORT LOCAL MODEL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    EmbeddedModelStatus.DOWNLOADING, EmbeddedModelStatus.CHECKING, EmbeddedModelStatus.VERIFYING -> {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cancel_embedded_model_download_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Cancel Import",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    EmbeddedModelStatus.READY -> {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_embedded_model_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Remove Model File",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    EmbeddedModelStatus.ERROR, EmbeddedModelStatus.INSUFFICIENT_STORAGE, EmbeddedModelStatus.CORRUPTED -> {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("retry_embedded_model_button")
                                .testTag("import_embedded_model_button"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanLuminous,
                                contentColor = CoreBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Retry Import",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    else -> {}
                }
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

@Composable
private fun MemoryDiagnosticsCard(
    settings: SummerSettings,
    diagnostics: AIDiagnostics,
    onClearMemories: () -> Unit,
    onClearConversation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_diagnostics_card"),
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
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Memory & Context",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateBright
                        )
                        Text(
                            text = "Deterministic Bounded Subsystem",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (settings.personalMemoryEnabled) CyanLuminous.copy(alpha = 0.2f) else CoreCharcoalElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (settings.personalMemoryEnabled) "ACTIVE" else "DISABLED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settings.personalMemoryEnabled) CyanBright else SlateMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            DiagnosticRow(label = "Persistent Storage", value = "On-Device SQLite (Room)")
            DiagnosticRow(label = "Context Bounding", value = "Max 5 memories / 6 dialogue turns")
            DiagnosticRow(label = "Relevance Engine", value = "Deterministic local lexical scoring")
            DiagnosticRow(label = "Memory Isolation", value = "Explicit intent required (Zero automatic leak)")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearConversation,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_conversation_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder)
                ) {
                    Text("Reset Dialogue", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onClearMemories,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_memories_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder)
                ) {
                    Text("Clear Memories", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun VoiceSettingsCard(
    settings: SummerSettings,
    diagnostics: AIDiagnostics,
    onSelectVoiceProfile: (VoiceProfileId) -> Unit,
    onSetSpeechSpeed: (Float) -> Unit,
    onPreviewVoice: () -> Unit
) {
    val isMale = settings.voiceProfileId.equals("MALE", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Voice Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateBright
                        )
                        Text(
                            text = "Conversational AI Speech",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanLuminous.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isMale) "MALE" else "FEMALE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "VOICE IDENTITY",
                style = MaterialTheme.typography.labelSmall,
                color = CyanLuminous
            )

            // Two first-class voice profiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelectVoiceProfile(VoiceProfileId.FEMALE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_profile_female_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isMale) CyanLuminous.copy(alpha = 0.15f) else CoreCharcoalElevated,
                        contentColor = if (!isMale) CyanBright else SlateLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isMale) CyanLuminous else CoreCharcoalBorder
                    )
                ) {
                    Text("Summer Female", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { onSelectVoiceProfile(VoiceProfileId.MALE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_profile_male_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMale) CyanLuminous.copy(alpha = 0.15f) else CoreCharcoalElevated,
                        contentColor = if (isMale) CyanBright else SlateLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isMale) CyanLuminous else CoreCharcoalBorder
                    )
                ) {
                    Text("Summer Male", style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                text = "SPEECH SPEED",
                style = MaterialTheme.typography.labelSmall,
                color = CyanLuminous
            )

            // Speed selector: Slow (0.8x), Normal (1.0x), Fast (1.25x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentSpeed = settings.speechSpeed
                val speeds = listOf(Pair("Slow", 0.8f), Pair("Normal", 1.0f), Pair("Fast", 1.25f))

                for ((label, speedVal) in speeds) {
                    val isSelected = (Math.abs(currentSpeed - speedVal) < 0.1f)
                    OutlinedButton(
                        onClick = { onSetSpeechSpeed(speedVal) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("speed_${label.lowercase()}_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) CyanLuminous.copy(alpha = 0.15f) else CoreCharcoalElevated,
                            contentColor = if (isSelected) CyanBright else SlateLight
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CyanLuminous else CoreCharcoalBorder
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            DiagnosticRow(
                label = "Active Provider",
                value = diagnostics.voiceDiagnostics.activeProvider.displayName
            )
            DiagnosticRow(
                label = "Resolved Voice",
                value = diagnostics.voiceDiagnostics.resolvedEngineVoice ?: "Default System Voice"
            )
            DiagnosticRow(
                label = "Offline Capable",
                value = if (diagnostics.voiceDiagnostics.isOfflineCapable) "Yes (100% On-Device)" else "No"
            )

            OutlinedButton(
                onClick = onPreviewVoice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("preview_voice_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanLuminous),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanLuminous.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Preview Voice Output", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun VisionDiagnosticsCard(
    diagnostics: AIDiagnostics
) {
    val vision = diagnostics.visionDiagnostics
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vision_diagnostics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f))
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Vision & Camera Diagnostics",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Architectural Foundation",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CoreCharcoalElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "FOUNDATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            DiagnosticRow(
                label = "Camera Hardware",
                value = if (vision.cameraHardwareAvailable) "Available" else "Unavailable"
            )
            DiagnosticRow(
                label = "Camera Permission",
                value = when (vision.cameraPermissionState) {
                    com.example.vision.CameraPermissionState.GRANTED -> "Granted"
                    com.example.vision.CameraPermissionState.DENIED -> "Not granted"
                    com.example.vision.CameraPermissionState.PERMANENTLY_DENIED -> "Permanently denied"
                    com.example.vision.CameraPermissionState.UNKNOWN -> "Unknown"
                }
            )
            DiagnosticRow(
                label = "Vision Engine",
                value = when (vision.activeProviderType) {
                    com.example.vision.VisionProviderType.STUB -> "Stub"
                    com.example.vision.VisionProviderType.ANDROID_CAMERA -> "Android Camera"
                    com.example.vision.VisionProviderType.LOCAL_VISION -> "Local Vision"
                    com.example.vision.VisionProviderType.ON_DEVICE_GENAI -> "On-Device GenAI"
                    com.example.vision.VisionProviderType.OPTIONAL_ONLINE -> "Optional Online"
                }
            )
            DiagnosticRow(
                label = "Live Vision",
                value = "Not implemented"
            )
        }
    }
}

@Composable
private fun SpeechInputDiagnosticsCard(
    diagnostics: AIDiagnostics
) {
    val speech = diagnostics.speechDiagnostics
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("speech_input_diagnostics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoreCharcoalSurface.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.6f))
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
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = CyanLuminous,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Speech Input",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Foreground Speech Recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateLight
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CoreCharcoalElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (speech.isAvailable) "READY" else "UNAVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (speech.isAvailable) CyanBright else SlateMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            DiagnosticRow(
                label = "Recognizer",
                value = speech.recognizerName
            )
            DiagnosticRow(
                label = "Availability",
                value = if (speech.isAvailable) "Available" else "Unavailable"
            )
            DiagnosticRow(
                label = "Microphone Permission",
                value = if (speech.microphonePermissionGranted) "Granted" else "Not granted"
            )
            DiagnosticRow(
                label = "Locale",
                value = speech.locale
            )
            DiagnosticRow(
                label = "Offline Capability",
                value = speech.offlineCapability.label
            )
            DiagnosticRow(
                label = "State",
                value = speech.state.name
            )
        }
    }
}


