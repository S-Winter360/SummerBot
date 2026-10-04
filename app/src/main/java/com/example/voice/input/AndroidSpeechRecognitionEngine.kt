package com.example.voice.input

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Concrete Android implementation backed by the platform [SpeechRecognizer] API.
 * Foreground-only: strictly activated on user demand and destroyed upon release.
 */
class AndroidSpeechRecognitionEngine(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : SpeechRecognitionEngine {

    private val _state = MutableStateFlow(SpeechRecognitionState.IDLE)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    override val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _diagnostics = MutableStateFlow(buildInitialDiagnostics())
    override val diagnostics: StateFlow<SpeechRecognitionDiagnostics> = _diagnostics.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var activeCallback: ((SpeechRecognitionResult) -> Unit)? = null

    private fun checkPermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun isPlatformAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
    }

    private fun detectOfflineCapability(): SpeechOfflineCapability {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechOfflineCapability.ON_DEVICE_CONFIRMED
                } else {
                    SpeechOfflineCapability.NETWORK_DEPENDENT
                }
            } catch (_: Throwable) {
                SpeechOfflineCapability.UNKNOWN
            }
        } else {
            SpeechOfflineCapability.UNKNOWN
        }
    }

    private fun buildInitialDiagnostics(): SpeechRecognitionDiagnostics {
        val available = isPlatformAvailable()
        val perm = checkPermission()
        val offline = detectOfflineCapability()
        val loc = try { Locale.getDefault().toLanguageTag() } catch (_: Throwable) { "en-US" }

        return SpeechRecognitionDiagnostics(
            recognizerName = "Android SpeechRecognizer",
            isAvailable = available,
            microphonePermissionGranted = perm,
            locale = loc,
            offlineCapability = offline,
            state = SpeechRecognitionState.IDLE,
            isListening = false,
            message = if (!available) "Speech recognition service not available on this device."
            else if (!perm) "Microphone permission required."
            else "Ready for foreground speech input."
        )
    }

    fun refreshDiagnostics() {
        val perm = checkPermission()
        val available = isPlatformAvailable()
        val offline = detectOfflineCapability()
        _diagnostics.update { current ->
            current.copy(
                isAvailable = available,
                microphonePermissionGranted = perm,
                offlineCapability = offline,
                state = _state.value,
                isListening = _state.value == SpeechRecognitionState.LISTENING
            )
        }
    }

    override suspend fun startListening(onResult: (SpeechRecognitionResult) -> Unit) {
        refreshDiagnostics()
        activeCallback = onResult

        if (!checkPermission()) {
            _state.value = SpeechRecognitionState.ERROR
            val res = SpeechRecognitionResult.PermissionDenied("Microphone permission RECORD_AUDIO not granted.")
            onResult(res)
            return
        }

        if (!isPlatformAvailable()) {
            _state.value = SpeechRecognitionState.UNAVAILABLE
            val res = SpeechRecognitionResult.Unavailable("Speech recognition is unavailable on this device.")
            onResult(res)
            return
        }

        withContext(Dispatchers.Main) {
            try {
                _state.value = SpeechRecognitionState.INITIALIZING
                _partialTranscript.value = ""

                // Clean up any stale recognizer instance
                speechRecognizer?.destroy()
                speechRecognizer = null

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                if (recognizer == null) {
                    _state.value = SpeechRecognitionState.UNAVAILABLE
                    val res = SpeechRecognitionResult.Unavailable("Failed to initialize system SpeechRecognizer.")
                    activeCallback?.invoke(res)
                    return@withContext
                }

                speechRecognizer = recognizer
                recognizer.setRecognitionListener(createListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                recognizer.startListening(intent)
                _state.value = SpeechRecognitionState.LISTENING
                refreshDiagnostics()
            } catch (t: Throwable) {
                _state.value = SpeechRecognitionState.ERROR
                refreshDiagnostics()
                val res = SpeechRecognitionResult.Error(
                    error = SpeechRecognitionError.CLIENT_ERROR,
                    message = t.message ?: "Failed to start speech recognizer",
                    throwable = t
                )
                activeCallback?.invoke(res)
            }
        }
    }

    override suspend fun stopListening() {
        withContext(Dispatchers.Main) {
            try {
                if (_state.value == SpeechRecognitionState.LISTENING) {
                    _state.value = SpeechRecognitionState.PROCESSING
                    refreshDiagnostics()
                    speechRecognizer?.stopListening()
                }
            } catch (_: Throwable) {
                // Ignore failure on stop
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _state.value = SpeechRecognitionState.STOPPED
                _partialTranscript.value = ""
                refreshDiagnostics()
            } catch (_: Throwable) {
                // Ignore cancel failure
            }
        }
    }

    override fun release() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                _state.value = SpeechRecognitionState.IDLE
                _partialTranscript.value = ""
                activeCallback = null
                refreshDiagnostics()
            } catch (_: Throwable) {
                // Ignore destroy failure
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechRecognitionState.LISTENING
                refreshDiagnostics()
            }

            override fun onBeginningOfSpeech() {
                _state.value = SpeechRecognitionState.LISTENING
                refreshDiagnostics()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level feedback hook
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio hook - strictly ignored for privacy
            }

            override fun onEndOfSpeech() {
                _state.value = SpeechRecognitionState.PROCESSING
                refreshDiagnostics()
            }

            override fun onError(errorCode: Int) {
                val mappedError = when (errorCode) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.PERMISSION_DENIED
                    SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionError.NO_MATCH
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionError.NO_SPEECH
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionError.NETWORK_REQUIRED
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError.BUSY
                    SpeechRecognizer.ERROR_AUDIO -> SpeechRecognitionError.AUDIO_ERROR
                    SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError.SERVICE_UNAVAILABLE
                    SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError.CLIENT_ERROR
                    else -> SpeechRecognitionError.UNKNOWN
                }

                _state.value = SpeechRecognitionState.ERROR
                _partialTranscript.value = ""
                refreshDiagnostics()

                val result = when (mappedError) {
                    SpeechRecognitionError.PERMISSION_DENIED -> SpeechRecognitionResult.PermissionDenied(mappedError.userMessage)
                    SpeechRecognitionError.NO_SPEECH -> SpeechRecognitionResult.NoSpeech(mappedError.userMessage)
                    SpeechRecognitionError.NO_MATCH -> SpeechRecognitionResult.NoMatch(mappedError.userMessage)
                    SpeechRecognitionError.SERVICE_UNAVAILABLE -> SpeechRecognitionResult.Unavailable(mappedError.userMessage)
                    else -> SpeechRecognitionResult.Error(
                        error = mappedError,
                        message = mappedError.userMessage
                    )
                }

                activeCallback?.invoke(result)
            }

            override fun onResults(results: Bundle?) {
                _state.value = SpeechRecognitionState.COMPLETED
                refreshDiagnostics()

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val primaryTranscript = matches?.firstOrNull()?.trim()

                if (!primaryTranscript.isNullOrBlank()) {
                    val conf = confidences?.firstOrNull()
                    val result = SpeechRecognitionResult.Success(
                        transcript = primaryTranscript,
                        confidence = if (conf != null && conf >= 0f) conf else null
                    )
                    _partialTranscript.value = ""
                    activeCallback?.invoke(result)
                } else {
                    _partialTranscript.value = ""
                    activeCallback?.invoke(SpeechRecognitionResult.NoMatch("No speech match found in results."))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim()
                if (!partial.isNullOrBlank()) {
                    _partialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Optional vendor extensions
            }
        }
    }
}
