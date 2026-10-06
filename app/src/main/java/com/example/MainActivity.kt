package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CurrentScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.MainSummerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SummerWinterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SummerWinterTheme {
                SummerApp()
            }
        }
    }
}

@Composable
fun SummerApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val summerState by viewModel.summerState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()
    val latestAudit by viewModel.latestAuditLog.collectAsStateWithLifecycle()
    val speech by viewModel.recentAssistantSpeech.collectAsStateWithLifecycle()
    val currentInteraction by viewModel.currentInteraction.collectAsStateWithLifecycle()
    val aiDiagnostics by viewModel.aiDiagnostics.collectAsStateWithLifecycle()
    val speechState by viewModel.speechRecognitionState.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialSpeechTranscript.collectAsStateWithLifecycle()

    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            CurrentScreen.MAIN -> {
                MainSummerScreen(
                    state = summerState,
                    networkState = networkState,
                    personality = viewModel.getPersonality(),
                    latestAudit = latestAudit,
                    recentAssistantSpeech = speech,
                    currentInteraction = currentInteraction,
                    onStateSelected = { viewModel.selectStateDemo(it) },
                    onSubmitQuery = { viewModel.submitQuery(it) },
                    onMicTrigger = { viewModel.triggerVoiceInteraction() },
                    onNavigateToSettings = { viewModel.navigateTo(CurrentScreen.SETTINGS) },
                    speechState = speechState,
                    partialSpeechTranscript = partialTranscript
                )
            }

            CurrentScreen.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    aiDiagnostics = aiDiagnostics,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onRefreshAIDiagnostics = { viewModel.refreshAICapabilities() },
                    onImportEmbeddedModel = { viewModel.importEmbeddedModel(it) },
                    onCancelEmbeddedModelDownload = { viewModel.cancelEmbeddedModelDownload() },
                    onDeleteEmbeddedModel = { viewModel.deleteEmbeddedModel() },
                    onClearMemories = { viewModel.clearAllMemories() },
                    onClearConversation = { viewModel.clearConversationHistory() },
                    onSelectVoiceProfile = { viewModel.selectVoiceProfile(it) },
                    onSetSpeechSpeed = { viewModel.setSpeechSpeed(it) },
                    onPreviewVoice = { viewModel.previewVoice() },
                    benchmarkRunner = viewModel.benchmarkRunner,
                    onRunBenchmark = { viewModel.runEmbeddedAIBenchmark() },
                    onCancelBenchmark = { viewModel.cancelEmbeddedAIBenchmark() },
                    onResetBenchmark = { viewModel.resetEmbeddedAIBenchmark() },
                    onNavigateBack = { viewModel.navigateTo(CurrentScreen.MAIN) }
                )
            }
        }
    }
}
