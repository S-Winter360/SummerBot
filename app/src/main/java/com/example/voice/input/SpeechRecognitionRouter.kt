package com.example.voice.input

import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Provider-selection and routing coordinator for speech recognition input.
 * Integrates directly with [SummerEventBus] to publish [SummerEvent.VoiceInput].
 */
class SpeechRecognitionRouter(
    val engine: SpeechRecognitionEngine,
    private val eventBus: SummerEventBus? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    val state: StateFlow<SpeechRecognitionState> = engine.state
    val partialTranscript: StateFlow<String> = engine.partialTranscript
    val diagnostics: StateFlow<SpeechRecognitionDiagnostics> = engine.diagnostics

    suspend fun startListening(onFinalTranscript: (String) -> Unit) {
        engine.startListening { result ->
            when (result) {
                is SpeechRecognitionResult.Success -> {
                    onFinalTranscript(result.transcript)
                    scope.launch {
                        eventBus?.publish(
                            SummerEvent.VoiceInput(
                                audioDataPreview = result.transcript,
                                source = "voice.recognition",
                                priority = EventPriority.HIGH
                            )
                        )
                    }
                }
                is SpeechRecognitionResult.NoSpeech -> {
                    // Ignored or logged gracefully
                }
                is SpeechRecognitionResult.NoMatch -> {
                    // Ignored or logged gracefully
                }
                is SpeechRecognitionResult.PermissionDenied,
                is SpeechRecognitionResult.Unavailable,
                is SpeechRecognitionResult.Error -> {
                    // Diagnostic state updated on engine
                }
            }
        }
    }

    suspend fun stopListening() {
        engine.stopListening()
    }

    fun cancel() {
        engine.cancel()
    }

    fun release() {
        engine.release()
    }
}
