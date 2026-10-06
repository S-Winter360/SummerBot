package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.actions.ActionAuditEntry
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.network.NetworkState
import com.example.ui.components.CapabilityAuditTicker
import com.example.ui.components.ConversationTranscript
import com.example.ui.components.SummerCoreOrb
import com.example.ui.components.formatTranscriptForClipboard
import com.example.ui.models.ConversationMessage
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreen
import com.example.voice.input.SpeechRecognitionState

/**
 * Summer's Home Screen — Organic Futurism.
 *
 * Refined under Phase 0J-R2:
 * 1. The living circle is the primary interaction surface (tappable to start/cancel speech recognition).
 * 2. Removed the conventional chatbot bottom input bar and explicit "IDLE" status pill / text.
 * 3. Incorporates the in-session persistent conversation transcript with developer clipboard export.
 * 4. Preserves full architectural state machine, audio permissions, and Organic Futurism aesthetics.
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
    speechState: SpeechRecognitionState = SpeechRecognitionState.IDLE,
    partialSpeechTranscript: String = "",
    conversationMessages: List<ConversationMessage> = emptyList(),
    onCoreOrbClick: (() -> Unit)? = null,
    onCopyTranscript: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline = networkState != NetworkState.OFFLINE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (onCoreOrbClick != null) onCoreOrbClick() else onMicTrigger()
        }
    }

    val handleCoreInteraction = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (onCoreOrbClick != null) onCoreOrbClick() else onMicTrigger()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleCopyTranscriptAction = {
        if (onCopyTranscript != null) {
            onCopyTranscript()
        } else {
            val textToCopy = formatTranscriptForClipboard(conversationMessages)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Summer Conversation Transcript", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Companion Header (Lighter, dignified, companion-oriented)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bespoke Summer Winter Logo Emblem
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CoreCharcoalElevated)
                        .border(1.5.dp, CyanLuminous.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_summer_winter_logo),
                        contentDescription = "Summer Winter Logo Emblem",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Summer Winter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
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
                            text = if (isOnline) "Cognitive systems active" else "Offline Local Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) SlateLight else SlateMuted
                        )
                    }
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

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Central Living AI Core — Primary Interaction Surface
        // Tapping this starts listening (when Idle/Error), stops listening (when Listening), or interrupts speech (when Speaking).
        SummerCoreOrb(
            state = state,
            size = 250.dp,
            onClick = handleCoreInteraction
        )

        // Live partial speech transcript preview while listening
        if (state is SummerState.Listening && partialSpeechTranscript.isNotBlank()) {
            Text(
                text = "“$partialSpeechTranscript”",
                style = MaterialTheme.typography.bodyMedium,
                color = CyanLuminous,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("live_speech_transcript")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Floating Thought / Latest Assistant Speech Surface
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("assistant_speech_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CoreCharcoalSurface.copy(alpha = 0.65f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = recentAssistantSpeech ?: "Hello. I'm here whenever you'd like to talk.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateBright,
                    lineHeight = 22.sp
                )
            }
        }

        // 4. In-Session Persistent Conversation Transcript
        ConversationTranscript(
            messages = conversationMessages,
            onCopyTranscript = handleCopyTranscriptAction,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 5. Capability Audit Ticker (Understated security feedback)
        CapabilityAuditTicker(
            latestAudit = latestAudit,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )
    }
}
